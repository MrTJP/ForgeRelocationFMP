/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocationfmp

import codechicken.lib.raytracer.ExtendedMOP
import codechicken.lib.render.{CCRenderState, TextureUtils}
import codechicken.lib.vec.Rotation._
import codechicken.lib.vec.Vector3._
import codechicken.lib.vec.{BlockCoord, Cuboid6, Rotation, Vector3}
import codechicken.microblock.CommonMicroblock
import codechicken.multipart.MultiPartRegistry.IPartConverter
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.world.WorldLib
import mrtjp.mcframes.api.MCFramesAPI.{instance => API}
import mrtjp.mcframes.api.{IFrame, IFramePlacement}
import mrtjp.relocation.api.ITileMover
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.{MovingObjectPosition, Vec3}
import net.minecraft.world.World

import scala.collection.JavaConversions._

class FramePart extends TMultiPart with IFrame with TCuboidPart with TNormalOcclusion with TIconHitEffects
{
    override def getType = "rfmp_frame"

    override def stickOut(w:World, x:Int, y:Int, z:Int, side:Int) = tile.partMap(side) match
    {
        case part:CommonMicroblock => part.getSize != 1
        case _ => true
    }
    override def stickIn(w:World, x:Int, y:Int, z:Int, side:Int) = stickOut(w, x, y, z, side)

    override def getStrength(mop:MovingObjectPosition, player:EntityPlayer) =
        player.getBreakSpeed(API.getFrameBlock, false, 0, mop.blockX, mop.blockY, mop.blockZ)/
            API.getFrameBlock.getBlockHardness(player.worldObj, mop.blockX, mop.blockY, mop.blockZ)

    override def getDrops = Seq(new ItemStack(API.getFrameBlock))
    override def pickItem(hit:MovingObjectPosition) = new ItemStack(API.getFrameBlock)

    override def getBounds = Cuboid6.full

    override def getOcclusionBoxes =
    {
        FramePart.sideOccludeTest match
        {
            case -1 => Seq()
            case s => Seq(FramePart.aBounds(s))
        }
    }

    def sideOcclusionTest(side:Int) =
    {
        FramePart.sideOccludeTest = side
        val fits = tile.canReplacePart(this, this)
        FramePart.sideOccludeTest = -1
        fits
    }

    def sideOcclusionMask =
    {
        var mask = 0
        for (s <- 0 until 6) if (sideOcclusionTest(s)) mask |= 1<<s
        mask
    }

    override def occlusionTest(npart:TMultiPart):Boolean =
    {
        if (npart.isInstanceOf[FramePart]) return false

        //modified normal occlusion test that also tests collision boxes
        if (FramePart.sideOccludeTest != -1)
        {
            var boxes = Seq[Cuboid6]()
            if(npart.isInstanceOf[JNormalOcclusion])
                boxes ++= npart.asInstanceOf[JNormalOcclusion].getOcclusionBoxes
            if(npart.isInstanceOf[JPartialOcclusion])
                boxes ++= npart.asInstanceOf[JPartialOcclusion].getPartialOcclusionBoxes
            boxes ++= npart.getCollisionBoxes

            NormalOcclusionTest(boxes, getOcclusionBoxes)
        }
        else super.occlusionTest(npart)
    }

    override def collisionRayTrace(from:Vec3, to:Vec3) =
    {
        API.raytraceFrame(x, y, z, ~sideOcclusionMask, from, to) match
        {
            case mop:MovingObjectPosition =>
                Cuboid6.full.setBlockBounds(tile.getBlockType)
                new ExtendedMOP(mop, 0, from.squareDistanceTo(mop.hitVec))
            case _ => null
        }
    }

    override def doesTick = false

    @SideOnly(Side.CLIENT)
    override def renderStatic(pos:Vector3, pass:Int) = pass match
    {
        case 0 =>
            TextureUtils.bindAtlas(0)
            CCRenderState.setBrightness(world, x, y, z)
            API.renderFrame(pos.x, pos.y, pos.z, ~sideOcclusionMask)
            true
        case _ => false
    }

    @SideOnly(Side.CLIENT)
    override def getBrokenIcon(side:Int) = API.getFrameBlock.getIcon(side, 0)
}

object FramePart
{
    var sideOccludeTest = -1

    var aBounds = new Array[Cuboid6](6)

    {
        val i = 4/16D
        val th = 1/16D
        for(s <- 0 until 6)
            aBounds(s) = new Cuboid6(i, 0, i, 1-i, th, 1-i)
                    .apply(sideRotations(s).at(center))
    }
}

object FrameBlockConverter extends IPartConverter
{
    override def blockTypes = Seq(API.getFrameBlock)

    override def convert(world:World, pos:BlockCoord) =
        if (world.getBlock(pos.x, pos.y, pos.z) == API.getFrameBlock) new FramePart else null
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
            if (!TileMultipart.canPlacePart(world, pos, part))
                return false

            if (!world.isRemote) TileMultipart.addPart(world, pos, part)
            if (!player.capabilities.isCreativeMode) item.stackSize-=1

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
                WorldLib.uncheckedRemoveTileEntity(w, x, y, z)
                WorldLib.uncheckedSetBlock(w, x, y, z, Blocks.air, 0)

                val bc = new BlockCoord(x, y, z).offset(side)
                WorldLib.uncheckedSetBlock(w, bc.x, bc.y, bc.z, b, meta)

                te.xCoord = bc.x
                te.yCoord = bc.y
                te.zCoord = bc.z
                WorldLib.uncheckedSetTileEntity(w, bc.x, bc.y, bc.z, te)
            case _ =>
        }
    }

    override def postMove(w:World, x:Int, y:Int, z:Int) = WorldLib.uncheckedGetTileEntity(w, x, y, z) match
    {
        case te:TileMultipart => te.onMoved()
        case _ =>
    }
}