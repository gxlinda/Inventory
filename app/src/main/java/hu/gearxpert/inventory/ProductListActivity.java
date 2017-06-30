package hu.gearxpert.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import hu.gearxpert.inventory.data.InventoryContract;
import hu.gearxpert.inventory.data.InventoryContract.ProductEntry;
import hu.gearxpert.inventory.data.ProductFileHelper;

public class ProductListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRODUCT_LOADER = 0;
    private static String DEFAULT_PRODUCT_PHOTO_URI = "android.resource://hu.gearxpert.inventory/drawable/img_empty_photo";

    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Setup FAB to open ProductDetailsActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProductListActivity.this, ProductDetailsActivity.class);
                startActivity(intent);
            }
        });

        ListView productListView = (ListView) findViewById(R.id.product_list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        //setup an adapter to create a list item for each row in the Cursor.
        //there is no data yet until the loader finishes, so pass in null for the Cursor.
        mCursorAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);

        //setup item click listener to call edit mode for a product
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ProductListActivity.this, ProductDetailsActivity.class);
                intent.setData(ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        //inicialize the query
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Helper method to insert hardcoded product data into the database. For debugging purposes only.
     */
    private void insertProduct() {
        // Create a ContentValues object where column names are the keys,
        // and dummy product attributes are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, "Huawei P9 Lite");
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, "gold, charger included");
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK, "100");
        values.put(ProductEntry.COLUMN_PRODUCT_UNIT, ProductEntry.UNIT_PC);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, "62000");
        values.put(ProductEntry.COLUMN_PRODUCT_CURRENCY, "HUF");
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO, DEFAULT_PRODUCT_PHOTO_URI);

        // Insert a new row for dummy data into the provider using the ContentResolver.
        // Use the {@link ProductEntry#CONTENT_URI} to indicate that we want to insert
        // into the products database table.
        // Receive the new content URI that will allow us to access dummy data in the future.
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

        Toast.makeText(this, "Dummy product added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_product_list.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_product_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertProduct();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_products:
                showDeleteAllConfirmationDialog();
                return true;
            //Respond to a click on the " Open supplier list" menu option
            case R.id.action_open_supplier_list:
                Intent intent = new Intent(ProductListActivity.this, SupplierListActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllProducts() {

        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.list_delete_all_products_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.list_delete_all_products_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {     //requires full names with table name!!!
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.PRODUCT_ID,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_UNIT,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_CURRENCY,
                InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME
        };

        switch (id) {
            case PRODUCT_LOADER:
                return new CursorLoader(
                        this,
                        ProductEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //update the cursorAdapter with this new cursor containing updated product data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    private void showDeleteAllConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete all products.
                deleteAllProducts();
                ProductFileHelper.deleteAllProductImages(getApplicationContext());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}