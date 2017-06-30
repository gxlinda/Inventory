package hu.gearxpert.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import hu.gearxpert.inventory.data.InventoryContract;
import hu.gearxpert.inventory.data.InventoryContract.SupplierEntry;

import static android.content.ContentUris.parseId;

public class SupplierDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_SUPPLIER_LOADER = 0;

    //Content URI for the existing supplier (null if it's a new supplier)
    private Uri mCurrentSupplierUri;

    private EditText mNameEditText;
    private EditText mAddressEditText;
    private EditText mPhoneEditText;
    private EditText mEmailEditText;
    String nameString;
    String emailString;
    String activityOrigin;

    //Boolean flag that keeps track of whether the supplier has been edited (true) or not (false)
    private boolean mSupplierHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mSupplierHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mSupplierHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_details);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new supplier or editing an existing one.
        Intent intent = getIntent();
        mCurrentSupplierUri = intent.getData();

        // If the intent DOES NOT contain a supplier content URI, then we know that we are
        // creating a new supplier.
        if (mCurrentSupplierUri == null) {
            // This is a new supplier, so change the app bar to say "Add supplier"
            setTitle(getString(R.string.title_add_new_supplier));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a supplier that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing supplier, so change app bar to say "Edit supplier"
            setTitle(getString(R.string.title_edit_supplier));

            // Initialize a loader to read the supplier data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_SUPPLIER_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.supplier_name_et);
        mAddressEditText = (EditText) findViewById(R.id.supplier_address_et);
        mPhoneEditText = (EditText) findViewById(R.id.supplier_phone_et);
        mEmailEditText = (EditText) findViewById(R.id.supplier_email_et);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mAddressEditText.setOnTouchListener(mTouchListener);
        mPhoneEditText.setOnTouchListener(mTouchListener);
        mEmailEditText.setOnTouchListener(mTouchListener);
    }

    /**
     * Get user input from editor and save supplier into database.
     */
    private void saveSupplier() {

        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String addressString = mAddressEditText.getText().toString().trim();
        String phoneString = mPhoneEditText.getText().toString().trim();

        // Create a ContentValues object where column names are the keys,
        // and supplier attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(SupplierEntry.COLUMN_SUPPLIER_NAME, nameString);
        values.put(SupplierEntry.COLUMN_SUPPLIER_ADDRESS, addressString);
        values.put(SupplierEntry.COLUMN_SUPPLIER_PHONE, phoneString);
        values.put(SupplierEntry.COLUMN_SUPPLIER_EMAIL, emailString);

        // Determine if this is a new or existing supplier by checking if mCurrentSupplierUri is null or not
        if (mCurrentSupplierUri == null) {
            // This is a NEW supplier, so insert a new supplier into the provider,
            // returning the content URI for the new supplier.

            InventoryUtils.Vars.NewSupplierId = -1;//alaphelyzetbe hozás, szükséges!
            //          SQLiteDatabase _DB = null;

            Uri newRow = getContentResolver().insert(SupplierEntry.CONTENT_URI, values);

            //puts the new record _ID to vars
            InventoryUtils.Vars.NewSupplierId = parseId(newRow);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newRow == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_supplier_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_supplier_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING supplier, so update the supplier with content URI: mCurrentSupplierUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentSupplierUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentSupplierUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_supplier_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_supplier_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_supplier_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new supplier, hide the "Delete" menu item.
        if (mCurrentSupplierUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    String validationMessage;

    public boolean dataValidation() {

        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        nameString = mNameEditText.getText().toString().trim();
        emailString = mEmailEditText.getText().toString().trim();

        validationMessage = "";
        boolean dataAreValid = true;
        if (TextUtils.isEmpty(nameString)) {
            dataAreValid = false;
            validationMessage = getString(R.string.valid_enter_name_supplier);
        }

        if (TextUtils.isEmpty(emailString)
                || IsEmailValid(emailString) == false
                ) {
            dataAreValid = false;
            validationMessage += getString(R.string.valid_email);
        }

        return dataAreValid;
    }

    /**
     * Validates pEmail...
     *
     * @param pEmail
     * @return
     */
    private boolean IsEmailValid(String pEmail) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (pEmail.matches(emailPattern) && pEmail.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean _ret = false;

        //region *VALIDATION

        boolean _valid = true;

        if (item.getItemId() == R.id.action_save) {//we validate only at Save menu option
            _valid = dataValidation();
        }

        if (!_valid)//if it is invalid, a Toast is shown
        {
            Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show();
            validationMessage = "";
            return false;
        }
        //endregion

        //region *MENU OPTIONS
        if (_valid) {
            // User clicked on a menu option in the app bar overflow menu
            switch (item.getItemId()) {
                case R.id.action_save: // Respond to a click on the "Save" menu option
                    // Save supplier to database
                    saveSupplier();
                    // Exit activity
                    finish();
                    _ret = true;
                    break;
                case R.id.action_delete: // Respond to a click on the "Delete" menu option
                    // Pop up confirmation dialog for deletion
                    showDeleteConfirmationDialog();
                    _ret = true;
                    break;
                case android.R.id.home: // Respond to a click on the "Up" arrow button in the app bar
                    // If the supplier hasn't changed, continue with navigating up to parent activity
                    // which is the {@link CatalogActivity}.
                    if (!mSupplierHasChanged) {
                        NavUtils.navigateUpFromSameTask(SupplierDetailsActivity.this);
                        _ret = true;
                    }

                    // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                    // Create a click listener to handle the user confirming that
                    // changes should be discarded.
                    if (mSupplierHasChanged) {
                        DialogInterface.OnClickListener discardButtonClickListener =
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // User clicked "Discard" button, navigate to parent activity.
                                        NavUtils.navigateUpFromSameTask(SupplierDetailsActivity.this);
                                    }
                                };

                        // Show a dialog that notifies the user they have unsaved changes
                        showUnsavedChangesDialog(discardButtonClickListener);
                    }

                    _ret = true;
                    break;
            }
        }
        //endregion

        return _ret;
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the supplier hasn't changed, continue with handling back button press
        if (!mSupplierHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_supplier_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the supplier.
                deleteSupplier();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the supplier.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the supplier in the database.
     */
    private void deleteSupplier() {
        long id = parseId(mCurrentSupplierUri);
        if (usingSupplier(id)) {
            Toast.makeText(this, getString(R.string.delete_supplier_impossible), Toast.LENGTH_SHORT).show();
        } else {
            // Only perform the delete if this is an existing supplier.
            if (mCurrentSupplierUri != null) {
                // Call the ContentResolver to delete the supplier at the given content URI.
                // Pass in null for the selection and selection args because the mCurrentSupplierUri
                // content URI already identifies the supplier that we want.
                int rowsDeleted = getContentResolver().delete(mCurrentSupplierUri, null, null);

                // Show a toast message depending on whether or not the delete was successful.
                if (rowsDeleted == 0) {
                    // If no rows were deleted, then there was an error with the delete.
                    Toast.makeText(this, getString(R.string.editor_delete_supplier_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the delete was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_delete_supplier_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }

            // Close the activity
            finish();
        }
    }


    /**
     * Checks if the supplier ID in use in the products's table supplier_id column at least once. If yes, it returns true.
     *
     * @param pSupplierId
     * @return
     */
    private boolean usingSupplier(long pSupplierId) {
        String selection = SupplierEntry.TABLE_NAME_SUPPLIERS + "." + InventoryContract.SupplierEntry.SUPPLIER_ID + "=?";
        String[] selectionArgs = {String.valueOf(pSupplierId)};

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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all supplier attributes, define a projection that contains
        // all columns from the supplier table
        String[] projection = {
                SupplierEntry.SUPPLIER_ID,
                SupplierEntry.COLUMN_SUPPLIER_NAME,
                SupplierEntry.COLUMN_SUPPLIER_ADDRESS,
                SupplierEntry.COLUMN_SUPPLIER_PHONE,
                SupplierEntry.COLUMN_SUPPLIER_EMAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentSupplierUri,         // Query the content URI for the current supplier
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of supplier attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_NAME);
            int addressColumnIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_ADDRESS);
            int phoneColumnIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_PHONE);
            int emailColumnIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_EMAIL);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String address = cursor.getString(addressColumnIndex);
            String phone = cursor.getString(phoneColumnIndex);
            String email = cursor.getString(emailColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mAddressEditText.setText(address);
            mPhoneEditText.setText(phone);
            mEmailEditText.setText(email);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mAddressEditText.setText("");
        mPhoneEditText.setText("");
        mEmailEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the supplier.
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