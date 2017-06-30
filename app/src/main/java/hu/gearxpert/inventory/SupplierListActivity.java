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

import static hu.gearxpert.inventory.data.InventoryContract.SupplierEntry;

public class SupplierListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SUPPLIER_LOADER = 0;

    SupplierCursorAdapter mSupplierCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_list);

        // Setup FAB to open ProductDetailsActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_supplier);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SupplierListActivity.this, SupplierDetailsActivity.class);
                startActivity(intent);
            }
        });

        ListView supplierListView = (ListView) findViewById(R.id.supplier_list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view_supplier);
        supplierListView.setEmptyView(emptyView);

        //setup an adapter to create a list item for each row in the Cursor.
        //there is no data yet until the loader finishes, so pass in null for the Cursor.
        mSupplierCursorAdapter = new SupplierCursorAdapter(this, null);
        supplierListView.setAdapter(mSupplierCursorAdapter);

        //setup item click listener to call edit mode for a product
        supplierListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(SupplierListActivity.this, SupplierDetailsActivity.class);
                intent.setData(ContentUris.withAppendedId(SupplierEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        //inicialize the query
        getLoaderManager().initLoader(SUPPLIER_LOADER, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Helper method to insert hardcoded supplier data into the database. For debugging purposes only.
     */
    private void insertSupplier() {
        // Create a ContentValues object where column names are the keys,
        // and dummy product attributes are the values.
        ContentValues values = new ContentValues();
        values.put(SupplierEntry.COLUMN_SUPPLIER_NAME, "GearXpert Ltd.");
        values.put(SupplierEntry.COLUMN_SUPPLIER_ADDRESS, "3580 Tiszaújváros, Árpád út 25. 3/1.");
        values.put(SupplierEntry.COLUMN_SUPPLIER_PHONE, "+36703669170");
        values.put(SupplierEntry.COLUMN_SUPPLIER_EMAIL, "order@gearxpert.hu");

        // Insert a new row for dummy data into the provider using the ContentResolver.
        // Use the {@link ProductEntry#CONTENT_URI} to indicate that we want to insert
        // into the products database table.
        // Receive the new content URI that will allow us to access dummy data in the future.
        Uri newUri = getContentResolver().insert(SupplierEntry.CONTENT_URI, values);

        Toast.makeText(this, "Dummy supplier added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_supplier_list.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_supplier_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_supplier_data:
                insertSupplier();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_suppliers:
                showDeleteAllConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Checks if the supplier ID in use in the products's table supplier_id column at least once. If yes, it returns true.
     *
     * @return
     */
    private boolean usingSuppliers() {
        String selection = InventoryContract.ProductEntry.TABLE_NAME_PRODUCTS + "." + InventoryContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID + ">?";
        String[] selectionArgs = {"0"};

        String[] projection = {"count(*) as productcount"};
        Cursor product = getContentResolver().query(
                InventoryContract.ProductEntry.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );
        product.moveToFirst();
        int _columnindex = product.getColumnIndex("productcount");
        int _productCount = product.getInt(_columnindex);
        return _productCount > 0;
    }

    private void deleteAllSuppliers() {

        if (usingSuppliers()) {
            Toast.makeText(this, getString(R.string.delete_all_supplier_impossible), Toast.LENGTH_SHORT).show();
        } else {

            int rowsDeleted = getContentResolver().delete(SupplierEntry.CONTENT_URI, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.list_delete_all_suppliers_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.list_delete_all_suppliers_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                SupplierEntry.SUPPLIER_ID,
                SupplierEntry.COLUMN_SUPPLIER_NAME,
                SupplierEntry.COLUMN_SUPPLIER_ADDRESS,
                SupplierEntry.COLUMN_SUPPLIER_PHONE,
                SupplierEntry.COLUMN_SUPPLIER_EMAIL
        };

        switch (id) {
            case SUPPLIER_LOADER:
                return new CursorLoader(
                        this,
                        SupplierEntry.CONTENT_URI,
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
        mSupplierCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //callback called when the data needs to be deleted
        mSupplierCursorAdapter.swapCursor(null);
    }

    private void showDeleteAllConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_suppliers_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete all suppliers.
                deleteAllSuppliers();
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