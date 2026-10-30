package org.xet.experiments.builder.algorithm.color;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ColorMap {
    BufferedImage image;

    // Blocks that can be used in images (light-emitting blocks are filtered out at runtime as well)
    private static final List<ImageColorEnum> PALETTE = Arrays.stream(ImageColorEnum.values())
            .filter(ImageColorEnum::isUsable)
            .toList();

    public ColorMap(BufferedImage image) {
        this.image = image;
    }

    public ImageColorEnum[][] getColorMap(int width, int height) {
        BufferedImage resized = null;
        try {
            resized = resizeImage(this.image, width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] data = resized.getRGB(0, 0, width, height, null, 0, width);
        Color[][] colors = new Color[width][height];
        for (int i = 0; i < data.length; i++) {
            int x = i % width;
            int y = i / width;
            colors[x][y] = new Color(data[i]);
        }

        ImageColorEnum[][] colorsFromEnum = new ImageColorEnum[width][height];
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                Color color = colors[i][j];
                colorsFromEnum[i][j] = getStringColorFromRGB(color.getRed(), color.getGreen(), color.getBlue());
            }
        }

        return colorsFromEnum;
    }

    private static ImageColorEnum getStringColorFromRGB(int r, int g, int b) {
        Color targetColor = new Color(r, g, b);
        List<ImageColorEnum> bestMatches = new ArrayList<>();
        double minDistance = Double.MAX_VALUE;

        for (ImageColorEnum entry : PALETTE) {
            double distance = calculateDistance(targetColor, entry.getColor());

            if (distance < minDistance) {
                minDistance = distance;
                bestMatches.clear();
                bestMatches.add(entry);
            } else if (distance == minDistance) {
                // e.g. concrete and concrete slab share the same colour, pick one of them at random
                bestMatches.add(entry);
            }
        }

        if (bestMatches.isEmpty()) {
            return ImageColorEnum.UNKNOWN;
        }
        return bestMatches.get(ThreadLocalRandom.current().nextInt(bestMatches.size()));
    }

    private static double calculateDistance(Color c1, Color c2) {
        // Standard Euclidean distance
        long rDiff = c1.getRed() - c2.getRed();
        long gDiff = c1.getGreen() - c2.getGreen();
        long bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        return resizedImage;
    }
}