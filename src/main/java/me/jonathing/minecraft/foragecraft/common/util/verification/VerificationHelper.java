package me.jonathing.minecraft.foragecraft.common.util.verification;

import net.minecraftforge.fml.*;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class VerificationHelper
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static SignatureInformation getSignatureInfo(ModFileInfo mod, String expectedFingerprint)
    {
        if (!FMLEnvironment.secureJarsEnabled)
        {
            return null;
        }

        return new SignatureInformation(mod.getCodeSigningFingerprint().orElse("UNSIGNED"), expectedFingerprint, mod.getTrustData().orElse("None"));
    }

    public static void validateMod(String modId, String expectedSHA256) throws ModLoadingException
    {
        if (!ModList.get().getModContainerById(modId).isPresent())
        {
            LOGGER.error(String.format("Could not validate mod %s since it is not present in the mod list!", modId));
            return;
        }

        ModContainer modContainer = ModList.get().getModContainerById(modId).get();
        SignatureInformation signatureInfo = VerificationHelper.getSignatureInfo(((ModFileInfo) modContainer.getModInfo().getOwningFile()), expectedSHA256);

        if (signatureInfo == null)
        {
            LOGGER.fatal("FML has decided that Java is unable to check any of your mods for valid signature data.");
            LOGGER.fatal("This is very bad news. Please consider updating your Java version or scan your computer for viruses.");
            new FingerprintMismatchException(modId).printStackTrace();
        }
        else
        {
            switch (signatureInfo.getErrorCode())
            {
                case 0:
                    LOGGER.info(String.format("Verification of mod %s successful. Here is the signature data just in case:", modId));
                    LOGGER.info(String.format(" - Expected Fingerprint: %s", signatureInfo.getExpectedFingerprint().toUpperCase()));
                    LOGGER.info(String.format(" - Actual Fingerprint:   %s", signatureInfo.getFingerprint().toUpperCase()));
                    LOGGER.info(String.format(" - Trust Data:           %s", signatureInfo.getTrustData()));
                    break;
                case 1:
                    LOGGER.fatal(String.format("YOU ARE PLAYING A BUILD OF MOD %s THAT HAS BEEN SIGNED WITH AN UNAUTHORIZED FINGERPRINT!", modId.toUpperCase()));
                    LOGGER.fatal("THIS IS VERY BAD NEWS. PLEASE RUN A COMPUTER VIRUS SCAN AND REPORT THIS TO THE MOD DEVELOPER AS SOON AS POSSIBLE.");
                    LOGGER.fatal(String.format(" - Expected Fingerprint: %s", signatureInfo.getExpectedFingerprint().toUpperCase()));
                    LOGGER.fatal(String.format(" - Actual Fingerprint:   %s", signatureInfo.getFingerprint().toUpperCase()));
                    LOGGER.fatal(String.format(" - Trust Data:           %s", signatureInfo.getTrustData()));
                    throw new ModLoadingException(
                            modContainer.getModInfo(),
                            ModLoadingStage.VALIDATE,
                            String.format("VerificationHelper failed to validate the certificate of mod %s. Please view the console for more information.", modId),
                            new FingerprintMismatchException(modId));
                case 2:
                    LOGGER.fatal(String.format("You are running an unsigned build of mod %s. If you do not know what you are doing, please close the game now and download a legitimate build from the mod author's distribution site of choice. Otherwise, please proceed with caution.", modId));
                    ModLoader.get().addWarning(new ModLoadingWarning(
                            modContainer.getModInfo(),
                            ModLoadingStage.VALIDATE,
                            String.format("You are running an unsigned build of mod %s. If you don't know what you are doing, close the game now and read the message in console.", modId),
                            new FingerprintMismatchException(modId)));
                    break;
                default:
                    // This case should never activate.
                    LOGGER.fatal(String.format("Something went wrong when trying to validate mod %s.", modId));
                    break;
            }
        }
    }
}