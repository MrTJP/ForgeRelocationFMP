/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocationfmp

import codechicken.multipart.{MultiPartRegistry, MultipartGenerator}
import mrtjp.mcframes.api.{IFrame, MCFramesAPI}
import mrtjp.relocation.api.RelocationAPI.{instance => API}
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class RelocationFMPProxy_server
{
    def preinit()
    {
        MCFramesAPI.instance.registerFramePlacement(FramePlacement)
        API.registerTileMover("FMP", "Tile mover for Forge Multipart", FMPTileHandler)
        API.registerMandatoryMover("mod:forgemultipartcbe", "FMP")
    }

    def init()
    {
        MultiPartRegistry.registerParts((_:ResourceLocation, _:Boolean) => new FramePart, Array(FramePart.partType))
        MultiPartRegistry.registerConverter(FrameBlockConverter)
    }

    def postinit(){}
}

class RelocationFMPProxy_client extends RelocationFMPProxy_server
{
    @SideOnly(Side.CLIENT)
    override def preinit() = super.preinit()

    @SideOnly(Side.CLIENT)
    override def init() = super.init()

    @SideOnly(Side.CLIENT)
    override def postinit() = super.postinit()
}

object RelocationFMPProxy extends RelocationFMPProxy_client