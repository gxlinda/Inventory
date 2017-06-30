package hu.gearxpert.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static hu.gearxpert.inventory.data.InventoryContract.ProductEntry;
import static hu.gearxpert.inventory.data.InventoryContract.SupplierEntry;

/**
 * Created by melinda.kostenszki on 2017.06.19..
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;

    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        addProductTable(db);
        addSupplierTable(db);
    }

    private void addProductTable(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the products table
        String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductEntry.TABLE_NAME_PRODUCTS + " ("
                + ProductEntry.PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_DESCRIPTION + " TEXT, "
                + ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK + " INTEGER NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_UNIT + " INTEGER NOT NULL DEFAULT 0, " //default value is 'pc'
                + ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL DEFAULT 0, " //default value is 0
                + ProductEntry.COLUMN_PRODUCT_CURRENCY + " TEXT NOT NULL DEFAULT 'HUF', " //default value is 'HUF'
                + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID + " INTEGER NULL, "
                + ProductEntry.COLUMN_PRODUCT_PHOTO + " TEXT);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    private void addSupplierTable(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the suppliers table
        String SQL_CREATE_SUPPLIERS_TABLE = "CREATE TABLE " + SupplierEntry.TABLE_NAME_SUPPLIERS + " ("
                + SupplierEntry.SUPPLIER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SupplierEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL, "
                + SupplierEntry.COLUMN_SUPPLIER_ADDRESS + " TEXT, "
                + SupplierEntry.COLUMN_SUPPLIER_PHONE + " TEXT, "
                + SupplierEntry.COLUMN_SUPPLIER_EMAIL + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_SUPPLIERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
