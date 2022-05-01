package net.minecraft.src;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.PixelFormat;

public class RenderEngine
{
    private HashMap textureMap = new HashMap();

    /** Texture contents map (key: texture name, value: int[] contents) */
    private HashMap textureContentsMap = new HashMap();

    /** A mapping from GL texture names (integers) to BufferedImage instances */
    private IntHashMap textureNameToImageMap = new IntHashMap();

    /** Stores the image data for the texture. */
    private IntBuffer imageData = GLAllocation.createDirectIntBuffer(4194304);

    /** A mapping from image URLs to ThreadDownloadImageData instances */
    private Map urlToImageDataMap = new HashMap();

    /** Reference to the GameSettings object */
    private GameSettings options;

    /** Texture pack */
    public TexturePackList texturePack;

    /** Missing texture image */
    private BufferedImage missingTextureImage = new BufferedImage(64, 64, 2);
    public final TextureMap textureMapBlocks;
    public final TextureMap textureMapItems;
    public int boundTexture;
    public static Logger log = Logger.getAnonymousLogger();
    private boolean initialized = false;

    public RenderEngine(TexturePackList par1TexturePackList, GameSettings par2GameSettings)
    {
        if (Config.isMultiTexture())
        {
            int var3 = Config.getAntialiasingLevel();
            Config.dbg("FSAA Samples: " + var3);

            try
            {
                Display.destroy();
                Display.create((new PixelFormat()).withDepthBits(24).withSamples(var3));
            }
            catch (LWJGLException var9)
            {
                Config.dbg("Error setting FSAA: " + var3 + "x");
                var9.printStackTrace();

                try
                {
                    Display.create((new PixelFormat()).withDepthBits(24));
                }
                catch (LWJGLException var8)
                {
                    var8.printStackTrace();

                    try
                    {
                        Display.create();
                    }
                    catch (LWJGLException var7)
                    {
                        var7.printStackTrace();
                    }
                }
            }
        }

        this.texturePack = par1TexturePackList;
        this.options = par2GameSettings;
        Graphics var10 = this.missingTextureImage.getGraphics();
        var10.setColor(Color.WHITE);
        var10.fillRect(0, 0, 64, 64);
        var10.setColor(Color.BLACK);
        int var4 = 10;
        int var5 = 0;

        while (var4 < 64)
        {
            String var6 = var5++ % 2 == 0 ? "missing" : "texture";
            var10.drawString(var6, 1, var4);
            var4 += var10.getFont().getSize();

            if (var5 % 2 == 0)
            {
                var4 += 5;
            }
        }

        var10.dispose();
        this.textureMapBlocks = new TextureMap(0, "terrain", "textures/blocks/", this.missingTextureImage);
        this.textureMapItems = new TextureMap(1, "items", "textures/items/", this.missingTextureImage);
    }

    public int[] getTextureContents(String par1Str)
    {
        ITexturePack var2 = this.texturePack.getSelectedTexturePack();
        int[] var3 = (int[])((int[])this.textureContentsMap.get(par1Str));

        if (var3 != null)
        {
            return var3;
        }
        else
        {
            int[] var5;

            try
            {
                InputStream var4 = var2.getResourceAsStream(par1Str);

                if (var4 == null)
                {
                    var5 = this.getImageContentsAndAllocate(this.missingTextureImage);
                }
                else
                {
                    var5 = this.getImageContentsAndAllocate(this.readTextureImage(var4));
                }

                this.textureContentsMap.put(par1Str, var5);
                return var5;
            }
            catch (IOException var6)
            {
                var6.printStackTrace();
                var5 = this.getImageContentsAndAllocate(this.missingTextureImage);
                this.textureContentsMap.put(par1Str, var5);
                return var5;
            }
        }
    }

    private int[] getImageContentsAndAllocate(BufferedImage par1BufferedImage)
    {
        return this.getImageContents(par1BufferedImage, new int[par1BufferedImage.getWidth() * par1BufferedImage.getHeight()]);
    }

    private int[] getImageContents(BufferedImage par1BufferedImage, int[] par2ArrayOfInteger)
    {
        int var3 = par1BufferedImage.getWidth();
        int var4 = par1BufferedImage.getHeight();
        par1BufferedImage.getRGB(0, 0, var3, var4, par2ArrayOfInteger, 0, var3);
        return par2ArrayOfInteger;
    }

    public void bindTexture(String par1Str)
    {
        this.bindTexture(this.getTexture(par1Str));
    }

    public void bindTexture(int par1)
    {
        if (par1 != this.boundTexture)
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, par1);
            this.boundTexture = par1;
        }
    }

    public void resetBoundTexture()
    {
        this.boundTexture = -1;
    }

    public int getTexture(String par1Str)
    {
        if (Config.isRandomMobs())
        {
            par1Str = RandomMobs.getTexture(par1Str);
        }

        if (par1Str.equals("/terrain.png"))
        {
            this.textureMapBlocks.getTexture().bindTexture(0);
            return this.textureMapBlocks.getTexture().getGlTextureId();
        }
        else if (par1Str.equals("/gui/items.png"))
        {
            this.textureMapItems.getTexture().bindTexture(0);
            return this.textureMapItems.getTexture().getGlTextureId();
        }
        else
        {
            Integer var2 = (Integer)this.textureMap.get(par1Str);

            if (var2 != null)
            {
                return var2.intValue();
            }
            else
            {
                String var3 = par1Str;

                try
                {
                    Reflector.callVoid(Reflector.ForgeHooksClient_onTextureLoadPre, new Object[] {par1Str});
                    int var4 = GLAllocation.generateTextureNames();
                    boolean var9 = par1Str.startsWith("%blur%");

                    if (var9)
                    {
                        par1Str = par1Str.substring(6);
                    }

                    boolean var6 = par1Str.startsWith("%clamp%");

                    if (var6)
                    {
                        par1Str = par1Str.substring(7);
                    }

                    InputStream var7 = this.texturePack.getSelectedTexturePack().getResourceAsStream(par1Str);

                    if (var7 == null)
                    {
                        this.setupTextureExt(this.missingTextureImage, var4, var9, var6);
                    }
                    else
                    {
                        this.setupTextureExt(this.readTextureImage(var7), var4, var9, var6);
                    }

                    this.textureMap.put(var3, Integer.valueOf(var4));
                    Reflector.callVoid(Reflector.ForgeHooksClient_onTextureLoad, new Object[] {par1Str, this.texturePack.getSelectedTexturePack()});
                    return var4;
                }
                catch (Exception var8)
                {
                    var8.printStackTrace();
                    int var5 = GLAllocation.generateTextureNames();
                    this.setupTexture(this.missingTextureImage, var5);
                    this.textureMap.put(par1Str, Integer.valueOf(var5));
                    return var5;
                }
            }
        }
    }

    /**
     * Copy the supplied image onto a newly-allocated OpenGL texture, returning the allocated texture name
     */
    public int allocateAndSetupTexture(BufferedImage par1BufferedImage)
    {
        int var2 = GLAllocation.generateTextureNames();
        this.setupTexture(par1BufferedImage, var2);
        this.textureNameToImageMap.addKey(var2, par1BufferedImage);
        return var2;
    }

    /**
     * Copy the supplied image onto the specified OpenGL texture
     */
    public void setupTexture(BufferedImage par1BufferedImage, int par2)
    {
        this.setupTextureExt(par1BufferedImage, par2, false, false);
    }

    public void setupTextureExt(BufferedImage par1BufferedImage, int par2, boolean par3, boolean par4)
    {
        this.bindTexture(par2);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        if (par3)
        {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        }

        if (par4)
        {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        }
        else
        {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }

        int var5 = par1BufferedImage.getWidth();
        int var6 = par1BufferedImage.getHeight();
        int[] var7 = new int[var5 * var6];
        par1BufferedImage.getRGB(0, 0, var5, var6, var7, 0, var5);

        if (this.options != null && this.options.anaglyph)
        {
            var7 = this.colorToAnaglyph(var7);
        }

        this.fixTransparency(var7);
        this.checkImageDataSize(var7.length);
        this.imageData.clear();
        this.imageData.put(var7);
        this.imageData.position(0).limit(var7.length);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, var5, var6, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, this.imageData);
    }

    private int[] colorToAnaglyph(int[] par1ArrayOfInteger)
    {
        int[] var2 = new int[par1ArrayOfInteger.length];

        for (int var3 = 0; var3 < par1ArrayOfInteger.length; ++var3)
        {
            int var4 = par1ArrayOfInteger[var3] >> 24 & 255;
            int var5 = par1ArrayOfInteger[var3] >> 16 & 255;
            int var6 = par1ArrayOfInteger[var3] >> 8 & 255;
            int var7 = par1ArrayOfInteger[var3] & 255;
            int var8 = (var5 * 30 + var6 * 59 + var7 * 11) / 100;
            int var9 = (var5 * 30 + var6 * 70) / 100;
            int var10 = (var5 * 30 + var7 * 70) / 100;
            var2[var3] = var4 << 24 | var8 << 16 | var9 << 8 | var10;
        }

        return var2;
    }

    public void createTextureFromBytes(int[] par1ArrayOfInteger, int par2, int par3, int par4)
    {
        this.bindTexture(par4);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        if (this.options != null && this.options.anaglyph)
        {
            par1ArrayOfInteger = this.colorToAnaglyph(par1ArrayOfInteger);
        }

        this.checkImageDataSize(par1ArrayOfInteger.length);
        this.imageData.clear();
        this.imageData.put(par1ArrayOfInteger);
        this.imageData.position(0).limit(par1ArrayOfInteger.length);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, par2, par3, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, this.imageData);
    }

    /**
     * Deletes a single GL texture
     */
    public void deleteTexture(int par1)
    {
        this.textureNameToImageMap.removeObject(par1);
        GL11.glDeleteTextures(par1);
    }

    /**
     * Takes a URL of a downloadable image and the name of the local image to be used as a fallback.  If the image has
     * been downloaded, returns the GL texture of the downloaded image, otherwise returns the GL texture of the fallback
     * image.
     */
    public int getTextureForDownloadableImage(String par1Str, String par2Str)
    {
        ThreadDownloadImageData var3 = (ThreadDownloadImageData)this.urlToImageDataMap.get(par1Str);

        if (var3 != null && var3.image != null && !var3.textureSetupComplete)
        {
            if (var3.textureName < 0)
            {
                var3.textureName = this.allocateAndSetupTexture(var3.image);
            }
            else
            {
                this.setupTexture(var3.image, var3.textureName);
            }

            var3.textureSetupComplete = true;
        }

        return var3 != null && var3.textureName >= 0 ? var3.textureName : (par2Str == null ? -1 : this.getTexture(par2Str));
    }

    /**
     * Checks if urlToImageDataMap has image data for the given key
     */
    public boolean hasImageData(String par1Str)
    {
        return this.urlToImageDataMap.containsKey(par1Str);
    }

    /**
     * Return a ThreadDownloadImageData instance for the given URL.  If it does not already exist, it is created and
     * uses the passed ImageBuffer.  If it does, its reference count is incremented.
     */
    public ThreadDownloadImageData obtainImageData(String par1Str, IImageBuffer par2IImageBuffer)
    {
        if (par1Str != null && par1Str.length() > 0 && Character.isDigit(par1Str.charAt(0)))
        {
            return null;
        }
        else
        {
            ThreadDownloadImageData var3 = (ThreadDownloadImageData)this.urlToImageDataMap.get(par1Str);

            if (var3 == null)
            {
                this.urlToImageDataMap.put(par1Str, new ThreadDownloadImageData(par1Str, par2IImageBuffer));
            }
            else
            {
                ++var3.referenceCount;
            }

            return var3;
        }
    }

    /**
     * Decrements the reference count for a given URL, deleting the image data if the reference count hits 0
     */
    public void releaseImageData(String par1Str)
    {
        ThreadDownloadImageData var2 = (ThreadDownloadImageData)this.urlToImageDataMap.get(par1Str);

        if (var2 != null)
        {
            --var2.referenceCount;

            if (var2.referenceCount == 0)
            {
                if (var2.textureName >= 0)
                {
                    this.deleteTexture(var2.textureName);
                }

                this.urlToImageDataMap.remove(par1Str);
            }
        }
    }

    public void updateDynamicTextures()
    {
        this.checkInitialized();
        this.textureMapBlocks.updateAnimations();
        this.textureMapItems.updateAnimations();
        this.resetBoundTexture();
        TextureAnimations.updateCustomAnimations();
    }

    /**
     * Call setupTexture on all currently-loaded textures again to account for changes in rendering options
     */
    public void refreshTextures()
    {
        Config.dbg("*** Reloading textures ***");
        Config.log("Texture pack: \"" + this.texturePack.getSelectedTexturePack().getTexturePackFileName() + "\"");
        CustomSky.reset();
        TextureAnimations.reset();
        WrUpdates.finishCurrentUpdate();
        ITexturePack var1 = this.texturePack.getSelectedTexturePack();
        this.refreshTextureMaps();
        Iterator var2 = this.textureNameToImageMap.getKeySet().iterator();
        BufferedImage var3;

        while (var2.hasNext())
        {
            int var4 = ((Integer)var2.next()).intValue();
            var3 = (BufferedImage)this.textureNameToImageMap.lookup(var4);
            this.setupTexture(var3, var4);
        }

        ThreadDownloadImageData var14;

        for (var2 = this.urlToImageDataMap.values().iterator(); var2.hasNext(); var14.textureSetupComplete = false)
        {
            var14 = (ThreadDownloadImageData)var2.next();
        }

        var2 = this.textureMap.keySet().iterator();
        String var5;

        while (var2.hasNext())
        {
            var5 = (String)var2.next();

            try
            {
                int var6 = ((Integer)this.textureMap.get(var5)).intValue();
                boolean var7 = var5.startsWith("%blur%");

                if (var7)
                {
                    var5 = var5.substring(6);
                }

                boolean var8 = var5.startsWith("%clamp%");

                if (var8)
                {
                    var5 = var5.substring(7);
                }

                BufferedImage var9 = this.readTextureImage(var1.getResourceAsStream(var5));
                this.setupTextureExt(var9, var6, var7, var8);
            }
            catch (FileNotFoundException var12)
            {
                ;
            }
            catch (Exception var13)
            {
                if (!"input == null!".equals(var13.getMessage()))
                {
                    var13.printStackTrace();
                }
            }
        }

        var2 = this.textureContentsMap.keySet().iterator();

        while (var2.hasNext())
        {
            var5 = (String)var2.next();

            try
            {
                var3 = this.readTextureImage(var1.getResourceAsStream(var5));
                this.getImageContents(var3, (int[])((int[])this.textureContentsMap.get(var5)));
            }
            catch (FileNotFoundException var10)
            {
                ;
            }
            catch (Exception var11)
            {
                if (!"input == null!".equals(var11.getMessage()))
                {
                    var11.printStackTrace();
                }
            }
        }

        Minecraft.getMinecraft().fontRenderer.readFontData();
        Minecraft.getMinecraft().standardGalacticFontRenderer.readFontData();
        TextureAnimations.update(this);
        CustomColorizer.update(this);
        CustomSky.update(this);
        RandomMobs.resetTextures();
        Config.updateTexturePackClouds();
        this.updateDynamicTextures();
    }

    /**
     * Returns a BufferedImage read off the provided input stream.  Args: inputStream
     */
    private BufferedImage readTextureImage(InputStream par1InputStream) throws IOException
    {
        BufferedImage var2 = ImageIO.read(par1InputStream);
        par1InputStream.close();
        return var2;
    }

    public void refreshTextureMaps()
    {
        this.textureMapBlocks.refreshTextures();
        this.textureMapItems.refreshTextures();
        this.resetBoundTexture();
        TextureUtils.update(this);
        NaturalTextures.update(this);
        ConnectedTextures.update(this);
    }

    public Icon getMissingIcon(int par1)
    {
        switch (par1)
        {
            case 0:
                return this.textureMapBlocks.getMissingIcon();

            case 1:
            default:
                return this.textureMapItems.getMissingIcon();
        }
    }

    protected BufferedImage readTextureImage(String var1) throws IOException
    {
        InputStream var2 = this.texturePack.getSelectedTexturePack().getResourceAsStream(var1);

        if (var2 == null)
        {
            return null;
        }
        else
        {
            BufferedImage var3 = ImageIO.read(var2);
            var2.close();
            return var3;
        }
    }

    public TexturePackList getTexturePack()
    {
        return this.texturePack;
    }

    public void checkInitialized()
    {
        if (!this.initialized)
        {
            Minecraft var1 = Config.getMinecraft();

            if (var1 != null)
            {
                this.initialized = true;
                Config.log("Texture pack: \"" + this.texturePack.getSelectedTexturePack().getTexturePackFileName() + "\"");
                CustomColorizer.update(this);
                CustomSky.update(this);
                TextureAnimations.update(this);
                Config.updateTexturePackClouds();
            }
        }
    }

    public void checkImageDataSize(int var1)
    {
        if (this.imageData == null || this.imageData.capacity() < var1)
        {
            var1 = TextureUtils.ceilPowerOfTwo(var1);
            this.imageData = GLAllocation.createDirectIntBuffer(var1);
        }
    }

    private void fixTransparency(int[] var1)
    {
        for (int var2 = 0; var2 < var1.length; ++var2)
        {
            int var3 = var1[var2] >> 24 & 255;

            if (var3 == 0)
            {
                var1[var2] = 0;
            }
        }
    }

    public void refreshBlockTextures()
    {
        Config.dbg("*** Reloading block textures ***");
        WrUpdates.finishCurrentUpdate();
        this.textureMapBlocks.refreshTextures();
        TextureUtils.update(this);
        NaturalTextures.update(this);
        ConnectedTextures.update(this);
        this.updateDynamicTextures();
    }
}
