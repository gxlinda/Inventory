package hu.gearxpert.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import static hu.gearxpert.inventory.data.InventoryContract.ProductEntry;
import static hu.gearxpert.inventory.data.InventoryContract.SupplierEntry;

/**
 * Created by melinda.kostenszki on 2017.06.19..
 */

public class InventoryProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    // Use an int for each URI we will run, this represents the different queries (for UriMatcher)
    private static final int PRODUCTS = 100;
    private static final int PRODUCT_ID = 101;
    private static final int SUPPLIERS = 200;
    private static final int SUPPLIER_ID = 201;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_SUPPLIERS, SUPPLIERS);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_SUPPLIERS + "/#", SUPPLIER_ID);
    }

    //Database helper object
    private InventoryDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // This cursor will hold the result of the query
        Cursor cursor;

        //Open database
        SQLiteDatabase _DB = null;
        _DB = mDbHelper.getReadableDatabase();

        //Create new querybuilder
        SQLiteQueryBuilder _QB = null;
        _QB = new SQLiteQueryBuilder();

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {

            case PRODUCTS:

                //Specify products table and add join to suppliers table (use full ID, with table names!)
                _QB.setTables(
                        ProductEntry.TABLE_NAME_PRODUCTS
                                + " LEFT OUTER JOIN " + SupplierEntry.TABLE_NAME_SUPPLIERS
                                + " ON " + ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID
                                + " = " + SupplierEntry.TABLE_NAME_SUPPLIERS + "." + SupplierEntry.SUPPLIER_ID
                );
                //Get cursor
                cursor = _QB.query(_DB, projection, selection, selectionArgs, null, null, ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_NAME + " ASC");

                break;


            case PRODUCT_ID:
                selection = ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                _QB.setTables(
                        ProductEntry.TABLE_NAME_PRODUCTS
                                + " LEFT OUTER JOIN " + SupplierEntry.TABLE_NAME_SUPPLIERS
                                + " ON " + ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID
                                + " = " + SupplierEntry.TABLE_NAME_SUPPLIERS + "." + SupplierEntry.SUPPLIER_ID
                );
                //Get cursor
                cursor = _QB.query(_DB, projection, selection, selectionArgs, null, null, null);

                break;
            case SUPPLIERS:

                cursor = _DB.query(SupplierEntry.TABLE_NAME_SUPPLIERS, projection, selection, selectionArgs,
                        null, null, SupplierEntry.TABLE_NAME_SUPPLIERS + "." + SupplierEntry.COLUMN_SUPPLIER_NAME + " ASC");

                break;
            case SUPPLIER_ID:

                selection = SupplierEntry.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = _DB.query(SupplierEntry.TABLE_NAME_SUPPLIERS, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        //set notification URI on the Cursor, so we know what content URI the Cursor was created for.
        //if the data URI changes, we will know we have to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            case SUPPLIERS:
                return insertSupplier(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Product requires a name");
        }

        // No need to check the description, any value is valid (including null).

        // If the quantity is provided, check that it's greater than or equal to 0
        Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK);
        if (quantity == null || (quantity != null && quantity < 0)) {
            throw new IllegalArgumentException("Product requires valid quantity");
        }

        // Check that the unit is valid
        String unit = values.getAsString(ProductEntry.COLUMN_PRODUCT_UNIT);
        if (unit == null || !ProductEntry.isValidUnit(unit)) {
            throw new IllegalArgumentException("Product requires valid unit");
        }

        // If the price is provided, check that it's greater than or equal to 0
        Integer price = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price == null || (price != null && price < 0)) {
            throw new IllegalArgumentException("Product requires valid price");
        }

        // Check that the currency is not null
        String currency = values.getAsString(ProductEntry.COLUMN_PRODUCT_CURRENCY);
        if (currency == null) {
            throw new IllegalArgumentException("Product requires a currency");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new product with the given values
        long id = database.insert(ProductEntry.TABLE_NAME_PRODUCTS, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //notify all listeners that the data has changed for the content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Insert a supplier into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertSupplier(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(SupplierEntry.COLUMN_SUPPLIER_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Supplier requires a name");
        }

        // No need to check the address, and the phone number, as any value is valid (including null).

        // Check that the email is not null
        String email = values.getAsString(SupplierEntry.COLUMN_SUPPLIER_EMAIL);
        if (name == null) {
            throw new IllegalArgumentException("Supplier requires an email address");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new supplier with the given values
        long id = database.insert(SupplierEntry.TABLE_NAME_SUPPLIERS, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //notify all listeners that the data has changed for the content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                selection = ProductEntry.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case SUPPLIERS:
                return updateSupplier(uri, contentValues, selection, selectionArgs);
            case SUPPLIER_ID:
                selection = SupplierEntry.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateSupplier(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update products in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more products).
     * Return the number of rows that were successfully updated.
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        // No need to check the description, any value is valid (including null).

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK)) {
            Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK);
            if (quantity == null || (quantity != null && quantity < 0)) {
                throw new IllegalArgumentException("Product requires valid quantity");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_UNIT)) {
            String unit = values.getAsString(ProductEntry.COLUMN_PRODUCT_UNIT);
            if (unit == null || !ProductEntry.isValidUnit(unit)) {
                throw new IllegalArgumentException("Product requires valid unit");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Integer price = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price == null || (price != null && price < 0)) {
                throw new IllegalArgumentException("Product requires valid price");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_CURRENCY)) {
            String currency = values.getAsString(ProductEntry.COLUMN_PRODUCT_CURRENCY);
            if (currency == null) {
                throw new IllegalArgumentException("Product requires a currency");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME_PRODUCTS, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Returns the number of database rows affected by the update statement
        return rowsUpdated;
    }

    /**
     * Update suppliers in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more suppliers).
     * Return the number of rows that were successfully updated.
     */
    private int updateSupplier(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(SupplierEntry.COLUMN_SUPPLIER_NAME)) {
            String name = values.getAsString(SupplierEntry.COLUMN_SUPPLIER_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Supplier requires a name");
            }
        }

        // No need to check the address, and the phone number, as any value is valid (including null).

        if (values.containsKey(SupplierEntry.COLUMN_SUPPLIER_EMAIL)) {
            String name = values.getAsString(SupplierEntry.COLUMN_SUPPLIER_EMAIL);
            if (name == null) {
                throw new IllegalArgumentException("Supplier requires an email address");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(SupplierEntry.TABLE_NAME_SUPPLIERS, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Returns the number of database rows affected by the update statement
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME_PRODUCTS, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME_PRODUCTS, selection, selectionArgs);
                break;
            case SUPPLIERS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(SupplierEntry.TABLE_NAME_SUPPLIERS, selection, selectionArgs);
                break;
            case SUPPLIER_ID:
                // Delete a single row given by the ID in the URI
                selection = SupplierEntry.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(SupplierEntry.TABLE_NAME_SUPPLIERS, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            case SUPPLIERS:
                return SupplierEntry.CONTENT_LIST_TYPE;
            case SUPPLIER_ID:
                return SupplierEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
