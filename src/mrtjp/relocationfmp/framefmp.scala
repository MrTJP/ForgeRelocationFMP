/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocationfmp

import codechicken.lib.raytracer.{ExtendedMOP, IndexedCuboid6}
import codechicken.lib.render.{CCRenderState, TextureUtils}
import codechicken.lib.vec.{BlockCoord, Cuboid6, Rotation, Vector3}
import codechicken.microblock.CommonMicroblock
import codechicken.multipart.MultiPartRegistry.IPartConverter
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.relocation.api.RelocationAPI.{instance => API}
import mrtjp.relocation.api.{IFrame, IFramePlacement, ITileMover}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.{MovingObjectPosition, Vec3}
import net.minecraft.world.World

import scala.collection.JavaConversions._

class FramePart extends TMultiPart with IFrame with TFacePart with TCuboidPart with TIconHitEffects
{
    override def getType = "rfmp_frame"

    override def latchSide(w:World, x:Int, y:Int, z:Int, side:Int) = tile.partMap(side) match
    {
        case part:CommonMicroblock => part.getSize != 1
        case _ => true
    }

    override def getStrength(mop:MovingObjectPosition, player:EntityPlayer) =
        player.getBreakSpeed(API.getFrameBlock, false, 0, mop.blockX, mop.blockY, mop.blockZ)/
            API.getFrameBlock.getBlockHardness(player.worldObj, mop.blockX, mop.blockY, mop.blockZ)

    override def getDrops = Seq(new ItemStack(API.getFrameBlock))
    override def pickItem(hit:MovingObjectPosition) = new ItemStack(API.getFrameBlock)

    override def getBounds = Cuboid6.full

    override def occlusionTest(npart:TMultiPart) = !npart.isInstanceOf[FramePart]

    override def collisionRayTrace(from:Vec3, to:Vec3) =
        API.raytraceFrame(x, y, z, from, to) match
        {
            case mop:MovingObjectPosition => new ExtendedMOP(mop, 0, from.squareDistanceTo(mop.hitVec))
            case _ => null
        }

    override def doesTick = false

    override def getSlotMask = 0

    override def solid(side:Int) = tile.partMap(side) match
    {
        case part:CommonMicroblock => true
        case _ => false
    }

    @SideOnly(Side.CLIENT)
    override def renderStatic(pos:Vector3, pass:Int) = pass match
    {
        case 0 =>
            TextureUtils.bindAtlas(0)
            CCRenderState.setBrightness(world, x, y, z)
            API.renderFrame(pos.x, pos.y, pos.z)
            true
        case _ => false
    }

    @SideOnly(Side.CLIENT)
    override def getBrokenIcon(side:Int) = API.getFrameBlock.getIcon(side, 0)
}

object FrameBlockConverter extends IPartConverter
{
    override def blockTypes = Seq(API.getFrameBlock)

    override def convert(world:World, pos:BlockCoord) =
        if (world.getBlock(pos.x, pos.y, pos.z) eq API.getFrameBlock) new FramePart else null
}

object FramePlacement extends IFramePlacement
{
    def getHitDepth(vhit:Vector3, side:Int):Double =
        vhit.copy.scalarProject(Rotation.axes(side))+(side%2^1)

    override def onItemUse(item:ItemStack, player:EntityPlayer, world:World, x:Int, y:Int, z:Int, side:Int, vhit:Vector3):Boolean =
    {
        val pos = new BlockCoord(x, y, z)
        val d = getHitDepth(vhit, side)

        def place():Boolean =
        {
            if (TileMultipart.getTile(world, pos) == null) return false
            val part = new FramePart
            if(!TileMultipart.canPlacePart(world, pos, part)) return false

            if(!world.isRemote) TileMultipart.addPart(world, pos, part)
            if(!player.capabilities.isCreativeMode) item.stackSize-=1

            true
        }

        if(d < 1 && place()) return true

        pos.offset(side)
        place()
    }
}

object FMPTileHandler extends ITileMover
{
    def getBlockInfo(world:World, x:Int, y:Int, z:Int) =
        (world.getBlock(x, y, z), world.getBlockMetadata(x, y, z),
            world.getTileEntity(x, y, z))

    override def canMove(w:World, x:Int, y:Int, z:Int) = w.getTileEntity(x, y, z) match
    {
        case t:TileMultipart => true
        case _ => false
    }

    override def move(w:World, x:Int, y:Int, z:Int, side:Int) =
    {
        val (b, meta, te) = getBlockInfo(w, x, y, z)
        te match
        {
            case t:TileMultipart =>
                API.uncheckedRemoveTileEntity(w, x, y, z)
                API.uncheckedSetBlock(w, x, y, z, Blocks.air, 0)

                val bc = new BlockCoord(x, y, z).offset(side)
                API.uncheckedSetBlock(w, bc.x, bc.y, bc.z, b, meta)

                te.xCoord = bc.x
                te.yCoord = bc.y
                te.zCoord = bc.z
                API.uncheckedSetTileEntity(w, bc.x, bc.y, bc.z, te)
            case _ =>
        }
    }

    override def postMove(w:World, x:Int, y:Int, z:Int) = API.uncheckedGetTileEntity(w, x, y, z) match
    {
        case te:TileMultipart => te.onMoved()
        case _ =>
    }
}