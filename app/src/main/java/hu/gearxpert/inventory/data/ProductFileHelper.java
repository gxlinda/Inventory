package hu.gearxpert.inventory.data;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by melinda.kostenszki on 2017.06.22..
 */

public class ProductFileHelper {
    private static final String PRODUCT_IMAGE_DIRECTORY = "product_images";
    private static final String IMAGE_FILE_EXTENSION = ".png";
    private static final Bitmap.CompressFormat BITMAP_COMPRESSION_FORMAT = Bitmap.CompressFormat.PNG;

    private static final String EDITOR_TEMPORARY_IMAGE_FILE_NAME = "editor_activity_temporary_image_file.png";

    //empty private constructor to prevent instantiation
    private ProductFileHelper() {
    }

    /**
     * method to generate product image filename including its containing directory
     *
     * @param productId id the ID of the product
     * @return String containing filename with directory name
     */
    private static String getProductImageFileName(long productId) {
        return PRODUCT_IMAGE_DIRECTORY + "/" + productId + IMAGE_FILE_EXTENSION;
    }

    /**
     * method to write bitmap to File
     *
     * @param imageFile is the File object pointing to destination file
     * @param bitmap    is the image data
     * @return true on success
     */
    private static boolean writeBitmapToFile(File imageFile, Bitmap bitmap) {
        if (!imageFile.canWrite()) {
            return false;
        }

        FileOutputStream outputStream = null;
        boolean success = false;

        try {
            outputStream = new FileOutputStream(imageFile.getAbsoluteFile());
            // PNG is a lossless format, the compression factor (100) is ignored
            bitmap.compress(BITMAP_COMPRESSION_FORMAT, 100, outputStream);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                success = false;
            }
        }

        return success;
    }

    /**
     * get product image File object from product URI, do not create
     *
     * @param context    is the calling context
     * @param productUri is the product URI
     * @return File on success, null on failure
     */
    public static File getProductImageFile(Context context, Uri productUri) {
        long productId = ContentUris.parseId(productUri);
        return getProductImageFile(context, productId, false);
    }

    /**
     * get product image File object from product URI, create if needed
     *
     * @param context    is the calling context
     * @param productUri is the product URI
     * @return File on success, null on failure
     */
    private static File getProductImageFile(Context context, Uri productUri, boolean create) {
        long productId = ContentUris.parseId(productUri);
        return getProductImageFile(context, productId, create);
    }

    /**
     * get product image File object from product ID, do not create
     *
     * @param context   is the calling context
     * @param productId is the product ID
     * @return File on success, null on failure
     */
    public static File getProductImageFile(Context context, long productId) {
        return getProductImageFile(context, productId, false);
    }

    /**
     * get product image File object from product ID, create if instructed
     *
     * @param context   is the calling context
     * @param productId is the product ID
     * @param create    if set to true, file will be created if not exists
     * @return File on success, null on failure
     */
    private static File getProductImageFile(Context context, long productId, boolean create) {

        String filename = getProductImageFileName(productId);
        File imageFile = new File(context.getFilesDir(), filename);
        File imageDirFile = imageFile.getParentFile();

        //  check directory if exists and create if needed
        if (!checkDirectory(imageDirFile, create)) {
            return null;
        }

        //  check file if exists and create if needed
        if (!checkFile(imageFile, create)) {
            return null;
        }
        return imageFile;
    }

    /**
     * write image bitmap to temporary file
     *
     * @param context is the calling context
     * @param bitmap  is the bitmap containing image data
     * @return File pointing to temporary image on success
     */
    public static File putProductTemporaryImageFile(Context context, Bitmap bitmap) {
        File temporaryImageFile = new File(context.getFilesDir(), EDITOR_TEMPORARY_IMAGE_FILE_NAME);

        //  check file if exists and create if needed, write bitmap
        if (checkFile(temporaryImageFile, true) && writeBitmapToFile(temporaryImageFile, bitmap)) {
            return temporaryImageFile;
        } else {
            return null;
        }
    }

    /**
     * method to get temporary image File object, do not create if not exitss
     *
     * @param context is the calling context
     * @return File object pointing to temporary image file on success
     */
    public static File getProductTemporaryImageFile(Context context) {
        return getProductTemporaryImageFile(context, false);
    }

    /**
     * method to get temporary image File object, if not exists, create
     *
     * @param context is the calling context
     * @param create  if set to true, file will be created
     * @return File object on success, null on failure
     */
    private static File getProductTemporaryImageFile(Context context, boolean create) {
        File temporaryImageFile = new File(context.getFilesDir(), EDITOR_TEMPORARY_IMAGE_FILE_NAME);

        //  check file if exists and create if needed
        if (!checkFile(temporaryImageFile, create)) {
            return null;
        }
        return temporaryImageFile;
    }

    /**
     * method to permanently store temporary image file to product image file
     *
     * @param context    is the calling context
     * @param productUri is the URI for the product
     * @return true on success
     */
    public static boolean saveTemporaryFileToProductImageFile(Context context, Uri productUri) {
        File temporaryFile = getProductTemporaryImageFile(context);
        File productImageFile = getProductImageFile(context, productUri, true);

        if (temporaryFile == null) {
            return false;
        }

        if (productImageFile == null) {
            return false;
        }

        try {
            copyFile(temporaryFile, productImageFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyFile(File sourceFile, File destinationFile) throws IOException {
        InputStream in = new FileInputStream(sourceFile);
        try {
            OutputStream out = new FileOutputStream(destinationFile);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * method to check if directory exists and create if not
     *
     * @param directory is the File object pointing to directory
     * @param create    if set to true, directory will be created
     * @return true on success
     */
    private static boolean checkDirectory(File directory, boolean create) {

        if (!directory.exists()) {
            if (create) {
                if (!directory.mkdirs()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * check if file exists, create if not
     *
     * @param file   is the File object pointing to wanted file
     * @param create if set to true, file will be created
     * @return true on success
     */
    private static boolean checkFile(File file, boolean create) {
        if (!file.exists()) {
            if (create) {
                try {
                    if (!file.createNewFile()) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * method to delete product image, product ID will be parsed from product URI
     *
     * @param context    is the calling context
     * @param productUri is the product URI
     * @return true on success
     */
    public static boolean deleteProductImage(Context context, Uri productUri) {
        long productId = ContentUris.parseId(productUri);
        return deleteProductImage(context, productId);
    }

    /**
     * method to delete product image
     *
     * @param context   is the calling context
     * @param productId is the product ID
     * @return true on success
     */
    private static boolean deleteProductImage(Context context, long productId) {
        File productImageFile = getProductImageFile(context, productId);
        return productImageFile != null && productImageFile.delete();
    }

    /**
     * method to delete all image files
     *
     * @param context is the calling context
     * @return true if deleting all files succeeded
     */
    public static boolean deleteAllProductImages(Context context) {
        File imageDirFile = new File(context.getFilesDir(), PRODUCT_IMAGE_DIRECTORY);

        if (imageDirFile.isDirectory()) {
            boolean success = true;
            for (File imageFile : imageDirFile.listFiles()) {
                if (!imageFile.delete()) {
                    //  make a note that at least one file failed to delete, but continue anyway,
                    //  try to clean up as many as possible
                    success = false;
                }
            }
            return success;
        } else {
            return false;
        }
    }
}
