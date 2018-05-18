/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocationfmp

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.{FMLPostInitializationEvent, FMLPreInitializationEvent}
import org.apache.logging.log4j.LogManager

@Mod(modid = RelocationFMPMod.modID, useMetadata = true, modLanguage = "scala")
object RelocationFMPMod
{
    final val modID = "relocation-fmp"
    final val modName = "RelocationFMPPlugin"
    final val version = "@VERSION@"
    final val buildnumber = "@BUILD_NUMBER@"

    val log = LogManager.getFormatterLogger(modID)

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        RelocationFMPProxy.preinit()
    }

    @Mod.EventHandler
    def init(event:FMLPreInitializationEvent)
    {
        RelocationFMPProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        RelocationFMPProxy.postinit()
    }
}