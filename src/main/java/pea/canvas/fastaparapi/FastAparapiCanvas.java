package pea.canvas.fastaparapi;

import pea.canvas.DrawingCanvas;
import pea.canvas.aparapi.RenderingKernel;
import pea.ga.Image;
import pea.pixelbuffer.PixelBufferByte;
import pea.pixelbuffer.PixelBufferShort;
import pea.util.ImageUtil;
import pea.util.SimpleColor;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class FastAparapiCanvas implements DrawingCanvas {

    private final int width;
    private final int height;

    private float[] shapeData;
    private final float[] backgroundData;

    private final FastRenderingKernel kernel;

    private SimpleColor fillColor = new SimpleColor();

    private int nextFreeIndex = -1;
    private Image image;

    public FastAparapiCanvas(final int width, final int height, int length) {
        this.width = width;
        this.height = height;
        shapeData = new float[length];
        backgroundData = new float[3];
        kernel = new FastRenderingKernel(new FastRenderingKernel.DataProvider() {
            @Override
            public float[] getBackgroundData() {
                return backgroundData;
            }

            @Override
            public float[] getShapeData() {
                return shapeData;
            }
        }, width, height);
    }

    public FastAparapiCanvas(final int width, final int height) {
        this(width, height, 0);
    }

    @Override
    public void begin() {
        nextFreeIndex = 0;
    }

    @Override
    public void end() {
        kernel.execute(((PixelBufferByte) image.pixels).getPixels());
        nextFreeIndex = -1;
    }

    @Override
    public void background(SimpleColor color) {
        backgroundData[0] = color.r;
        backgroundData[1] = color.g;
        backgroundData[2] = color.b;
    }

    @Override
    public void circle(float x, float y, float r) {
        int i = nextFreeIndex;
        ensureCapacity(i + 8);
        shapeData[i] = 0;
        shapeData[i + 1] = fillColor.r;
        shapeData[i + 2] = fillColor.g;
        shapeData[i + 3] = fillColor.b;
        shapeData[i + 4] = fillColor.a;
        shapeData[i + 5] = x;
        shapeData[i + 6] = y;
        shapeData[i + 7] = r * r;
        nextFreeIndex = i + 8;
    }

    @Override
    public void rect(float x, float y, float w, float h) {
        int i = nextFreeIndex;
        ensureCapacity(i + 9);
        shapeData[i] = 1;
        shapeData[i + 1] = fillColor.r;
        shapeData[i + 2] = fillColor.g;
        shapeData[i + 3] = fillColor.b;
        shapeData[i + 4] = fillColor.a;
        shapeData[i + 5] = x;
        shapeData[i + 6] = y;
        shapeData[i + 7] = w;
        shapeData[i + 8] = h;
        nextFreeIndex = i + 9;
    }

    @Override
    public void ellipse(float x, float y, float rX, float rY) {
        int i = nextFreeIndex;
        ensureCapacity(i + 9);
        shapeData[i] = 2;
        shapeData[i + 1] = fillColor.r;
        shapeData[i + 2] = fillColor.g;
        shapeData[i + 3] = fillColor.b;
        shapeData[i + 4] = fillColor.a;
        shapeData[i + 5] = x;
        shapeData[i + 6] = y;
        shapeData[i + 7] = rX;
        shapeData[i + 8] = rY;
        nextFreeIndex = i + 9;
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float strokeWeight) {
        int i = nextFreeIndex;
        ensureCapacity(i + 10);
        shapeData[i] = 3;
        shapeData[i + 1] = fillColor.r;
        shapeData[i + 2] = fillColor.g;
        shapeData[i + 3] = fillColor.b;
        shapeData[i + 4] = fillColor.a;
        shapeData[i + 5] = x1;
        shapeData[i + 6] = y1;
        shapeData[i + 7] = x2;
        shapeData[i + 8] = y2;
        shapeData[i + 9] = strokeWeight;
        nextFreeIndex = i + 10;
    }

    @Override
    public void polygon(float[] x, float[] y) {
        int i = nextFreeIndex;
        int vertices = x.length;
        int newCapacity = i + 6 + vertices * 2;
        ensureCapacity(newCapacity);
        shapeData[i] = 4;
        shapeData[i + 1] = fillColor.r;
        shapeData[i + 2] = fillColor.g;
        shapeData[i + 3] = fillColor.b;
        shapeData[i + 4] = fillColor.a;
        shapeData[i + 5] = vertices;
        for (int j = 0; j < vertices; j++) {
            shapeData[j + 6 + i] = x[j];
            shapeData[j + vertices + 6 + i] = y[j];
        }
        nextFreeIndex = newCapacity;
    }

    @Override
    public void fill(SimpleColor color) {
        this.fillColor.r = color.r;
        this.fillColor.g = color.g;
        this.fillColor.b = color.b;
        this.fillColor.a = color.a;
    }

    private void ensureCapacity(int capacity) {
        if (shapeData.length < capacity) {
            float[] newArray = new float[capacity];
            System.arraycopy(shapeData, 0, newArray, 0, shapeData.length);
            newArray[shapeData.length] = -1;
            shapeData = newArray;
        }
    }

    @Override
    public PixelBufferShort getPixels() {
        PixelBufferShort buffer = new PixelBufferShort();
        short[] pixels = new short[width * height * 3];
        buffer.setPixels(pixels);
        RenderingKernel kernel = new RenderingKernel(new RenderingKernel.DataProvider() {
            @Override
            public float[] getBackgroundData() {
                return backgroundData;
            }

            @Override
            public float[] getShapeData() {
                return shapeData;
            }
        }, width, height);
        kernel.execute(buffer);
        kernel.dispose();
        return buffer;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setImage(Image image) {
        if (image != this.image) {
            this.image = image;
            kernel.setImageData(((PixelBufferByte) this.image.pixels).getPixels());
        }
    }

    public static BufferedImage pixelsToImage(short[] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        byte[] imagePixels = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            imagePixels[i] = fromColor(pixels[i]);
        }

        image.getRaster().setDataElements(0, 0, width, height, imagePixels);

        return image;
    }

    public FastRenderingKernel getKernel() {
        return kernel;
    }

    @Override
    public BufferedImage toImage() {
        return pixelsToImage((getPixels()).getPixels(), width, height);
    }

    private static byte fromColor(short col) {
        float color = ((col + 32768f) / 65535f);
        int temp = Math.round(color * 255f);
        return (byte) (temp & 0xff);
    }

    @Override
    public Image createCompatibleImage(BufferedImage image) {
        return createImage(image);
    }

    public static Image createImage(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            image = ImageUtil.convert(image, BufferedImage.TYPE_3BYTE_BGR);
        }

        byte[] imagePixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        PixelBufferByte buffer = new PixelBufferByte();
        buffer.setPixels(imagePixels);
        Image img = new Image(image.getWidth(), image.getHeight(), buffer);
        return img;
    }

    public static short byteToShort(byte color) {
        return (short) (((((int) color & 0xff) / 255f) * 65535) - 32768);
    }

    public double getDifference() {
        return kernel.getDifference();
    }
}
