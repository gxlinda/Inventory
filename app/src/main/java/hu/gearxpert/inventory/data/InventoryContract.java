package hu.gearxpert.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by melinda.kostenszki on 2017.06.15..
 */

public class InventoryContract {

    private InventoryContract() {
    }

    public static final String CONTENT_AUTHORITY = "hu.gearxpert.inventory";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PRODUCTS = "products";
    public static final String PATH_SUPPLIERS = "suppliers";


    //Inner class that defines constant values for the products database table.
    public static final class ProductEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        //The MIME type of the #CONTENT_URI for a list of products
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        //The MIME type of the #CONTENT_URI for a single product
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        //details of PRODUCTS table
        public static final String TABLE_NAME_PRODUCTS = "products";

        public static final String PRODUCT_ID = BaseColumns._ID;
        public static final String COLUMN_PRODUCT_NAME = "name";
        public static final String COLUMN_PRODUCT_DESCRIPTION = "description";
        public static final String COLUMN_PRODUCT_QUANTITY_ON_STOCK = "quantity";
        public static final String COLUMN_PRODUCT_UNIT = "unit";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_PRODUCT_CURRENCY = "currency";
        public static final String COLUMN_PRODUCT_PHOTO = "photo";
        public static final String COLUMN_PRODUCT_SUPPLIER_ID = "supplier_id";

        //entries for unit
        public static final String UNIT_PC = "pc";
        public static final String UNIT_KG = "kg";
        public static final String UNIT_L = "l";

        public static boolean isValidUnit(String unit) {
            if (unit == UNIT_PC || unit == UNIT_KG || unit == UNIT_L) {
                return true;
            }
            return false;
        }
    }

    //Inner class that defines constant values for the suppliers database table.
    public static final class SupplierEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_SUPPLIERS);

        //The MIME type of the #CONTENT_URI for a list of suppliers
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUPPLIERS;

        //The MIME type of the #CONTENT_URI for a single supplier
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUPPLIERS;

        //details of SUPPLIERS table
        public static final String TABLE_NAME_SUPPLIERS = "suppliers";

        public static final String SUPPLIER_ID = BaseColumns._ID;
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";
        public static final String COLUMN_SUPPLIER_ADDRESS = "address";
        public static final String COLUMN_SUPPLIER_PHONE = "phone";
        public static final String COLUMN_SUPPLIER_EMAIL = "email";
    }
}
