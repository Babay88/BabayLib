package ru.babay.lib.transport;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.os.*;
import android.util.Log;
import com.android.vending.billing.IInAppBillingService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.model.PurchaseItem;
import ru.babay.lib.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 23.10.13
 * Time: 6:44
 */
public class PurchaseManager {

    private static final String IN_APP_ITEMS = "inapp";
    private static final int RESULT_OK = 0;


    public static final int REQUEST_CODE = 1898;

    private final String[] skus;
    private boolean inappAvailable;
    private boolean subscriptionAvailable;
    private IInAppBillingService mService;
    final private ArrayList<PurchaseItem> inAppItems = new ArrayList<PurchaseItem>();
    private boolean updated;
    ManagerListener managerListener;
    String saveFilePath;
    boolean noPrices;
    boolean updatingItems;
    boolean iAmDead;

    Activity activity;

    public PurchaseManager(Activity activity, String saveFilePath, String[] skus) {
        this.activity = activity;
        this.skus = skus;
        this.saveFilePath = saveFilePath;

        ArrayList<PurchaseItem> items = loadFromFile();
        if (items != null)
            inAppItems.addAll(items);
        if (inAppItems.size() == 0) {
            for (int i = 0; i < skus.length; i++)
                inAppItems.add(new PurchaseItem(skus[i], null));
            noPrices = true;
        }

        activity.bindService(new
                Intent("com.android.vending.billing.InAppBillingService.BIND"),
                mServiceConn, Context.BIND_AUTO_CREATE);
    }


    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            try {
                inappAvailable = mService.isBillingSupported(3, activity.getPackageName(), IN_APP_ITEMS) == 0;
                subscriptionAvailable = mService.isBillingSupported(3, activity.getPackageName(), "subs") == 0;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            onConnectedToService(mService);
        }
    };

    public void loadOwnedItems() {
        new AsyncTask<Object, Object, ArrayList<PurchaseItem>>() {
            @Override
            protected ArrayList<PurchaseItem> doInBackground(Object... params) {
                try {
                    Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), IN_APP_ITEMS, null);
                    int response = ownedItems.getInt("RESPONSE_CODE");
                    if (response == RESULT_OK) {
                        int fakes = 0;
                        ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                        String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
                        ArrayList<PurchaseItem> items = new ArrayList<PurchaseItem>(ownedItems.size());

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            String purchaseData = purchaseDataList.get(i);
                            String signature = signatureList.get(i);
                            if (!checkSecurity(purchaseData, signature)) {
                                fakes++;
                                continue;
                            }

                            String sku = ownedSkus.get(i);
                            try {
                                JSONObject obj = new JSONObject(purchaseData);
                                String token = obj.getString("purchaseToken");
                                PurchaseItem item = new PurchaseItem(sku, null);
                                item.setPurchaseToken(token);
                                items.add(item);
                            } catch (JSONException e) {
                            }
                        }

                        if (fakes > 0)
                            if (managerListener != null)
                                managerListener.onFakeException(fakes);
                        return items;

                        // if continuationToken != null, call getPurchases again
                        // and pass in the token to retrieve more items
                    }
                } catch (DeadObjectException e){
                    Log.e(Settings.TAG, e.getMessage(), e);
                    iAmDead = true;
                }
                catch (RemoteException e) {
                    Log.e(Settings.TAG, e.getMessage(), e);
                    if (!Settings.DEBUG)
                        BugHandler.sendException(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<PurchaseItem> items) {
                onOwnedItemsLoaded(items);
            }
        }.execute();
    }

    public boolean isDead(){
        return iAmDead;
    }

    public void consumePurchase(final PurchaseItem item) {
        new AsyncTask<Object, Object, Integer>() {
            @Override
            protected Integer doInBackground(Object... params) {
                try {
                    return mService.consumePurchase(3, activity.getPackageName(), item.getPurchaseToken());
                } catch (RemoteException e) {
                    Log.e(Settings.TAG, e.getMessage(), e);
                }
                return -1;
            }

            @Override
            protected void onPostExecute(Integer response) {
                if (response == RESULT_OK)
                    onConsume(item);
                else
                    onConsumeFailed(item);
            }
        }.execute();
    }

    public boolean purchase(PurchaseItem item) {
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, activity.getPackageName(),
                    item.getSku(), IN_APP_ITEMS, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            if (buyIntentBundle.getInt("RESPONSE_CODE") != RESULT_OK)
                return false;
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), 0, 0, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(Settings.TAG, e.getMessage(), e);
            if (!Settings.DEBUG)
                BugHandler.sendException(e);
            return false;
        } catch (IntentSender.SendIntentException e) {
            Log.e(Settings.TAG, e.getMessage(), e);
            if (!Settings.DEBUG)
                BugHandler.sendException(e);
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String data = dataIntent.getStringExtra("INAPP_PURCHASE_DATA");
                String dataSignature = dataIntent.getStringExtra("INAPP_DATA_SIGNATURE");
                if (!checkSecurity(data, dataSignature)) {
                    if (managerListener != null)
                        managerListener.onFakeException(1);
                    return;
                }

                try {
                    JSONObject jsData = new JSONObject(data);
                    String sku = jsData.getString("productId");
                    PurchaseItem item = new PurchaseItem(sku, null);
                    item.setPurchaseToken(jsData.getString("purchaseToken"));
                    PurchaseItem sourceItem = findItemBySku(sku);
                    if (sourceItem != null)
                        item.setPrice(sourceItem.getPrice());
                    onItemBought(item);
                } catch (JSONException e) {
                    Log.e(Settings.TAG, e.getMessage(), e);
                    if (!Settings.DEBUG)
                        BugHandler.sendException(e);
                }
            } else {
                onBuyItemCancelled();
            }
        }
    }

    protected void onConnectedToService(IInAppBillingService mService){
        if (noPrices)
            updateItems(skus);
    }

    protected void onItemBought(PurchaseItem item) {
        if (managerListener != null)
            managerListener.onPurchase(item);
    }

    protected void onBuyItemCancelled() {
        if (managerListener != null)
            managerListener.onPurchaseCancel();
    }

    protected void onOwnedItemsLoaded(ArrayList<PurchaseItem> items) {
        if (managerListener != null)
            managerListener.onLoadedboughtItems(items);
    }

    protected void onConsume(PurchaseItem item) {
        if (managerListener != null)
            managerListener.onConsume(item);
    }

    protected void onConsumeFailed(PurchaseItem item) {
        if (managerListener != null)
            managerListener.onConsumeFail(item);
    }

    protected PurchaseItem findItemBySku(String sku) {
        synchronized (inAppItems) {
            for (int i = 0; i < inAppItems.size(); i++) {
                if (inAppItems.get(i).getSku().equals(sku))
                    return inAppItems.get(i);
            }
        }
        return null;
    }

    public ArrayList<PurchaseItem> getInAppItems() {
        return inAppItems;
    }

    public void updateItems(final String[] itemIds) {
        if (updated || updatingItems || mService == null)
            return;

        updatingItems = true;
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                ArrayList<String> ids = new ArrayList<String>();
                ids.addAll(Arrays.asList(itemIds));

                Bundle querySkus = new Bundle();
                querySkus.putStringArrayList("ITEM_ID_LIST", ids);
                try {
                    Bundle skuDetails = mService.getSkuDetails(3, activity.getPackageName(), IN_APP_ITEMS, querySkus);

                    int response = skuDetails.getInt("RESPONSE_CODE");
                    if (response == RESULT_OK) {
                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                        ArrayList<PurchaseItem> newItems = new ArrayList<PurchaseItem>(responseList.size());
                        for (String thisResponse : responseList) {
                            JSONObject object = new JSONObject(thisResponse);
                            String sku = object.getString("productId");
                            String price = object.getString("price");
                            String title = object.getString("title");
                            String description = object.getString("description");
                            PurchaseItem item = new PurchaseItem(sku, price);
                            item.setTitle(title);
                            item.setDescription(description);
                            newItems.add(item);
                        }
                        if (!newItems.equals(inAppItems)) {
                            synchronized (inAppItems) {
                                inAppItems.clear();
                                inAppItems.addAll(newItems);
                            }
                            saveItems(newItems);
                            if (managerListener != null)
                                managerListener.onDataUpdated(newItems);
                        }
                        updated = true;
                    }
                } catch (RemoteException e) {
                } catch (JSONException e) {
                    Log.e(Settings.TAG, e.getMessage(), e);
                    if (managerListener != null)
                        managerListener.onErrorUpdating(e);
                }
                updatingItems = false;
                return null;
            }

        }.execute();

    }

    boolean checkSecurity(String responce, String signature) {
        //TODO: implement
        return true;
    }

    ArrayList<PurchaseItem> loadFromFile() {
        File file = activity.getFilesDir();
        file.mkdir();
        file = new File(file, saveFilePath);
        if (file.exists())
            try {
                FileInputStream stream = new FileInputStream(file);
                String data = Util.readFully(stream);
                stream.close();
                JSONArray source = new JSONArray(data);
                return PurchaseItem.fromJson(source);
            } catch (JSONException e) {
            } catch (IOException e) {
            }
        return null;
    }

    void saveItems(ArrayList<PurchaseItem> items) {
        File file = activity.getFilesDir();
        file.mkdir();
        file = new File(file, saveFilePath);
        try {
            OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(file));
            JSONArray data = PurchaseItem.toJsonArray(items);
            stream.write(data.toString());
            stream.flush();
            stream.close();
        } catch (JSONException e) {
        } catch (IOException e) {
        }
    }

    public void setManagerListener(ManagerListener managerListener) {
        this.managerListener = managerListener;
    }

    public Activity getActivity() {
        return activity;
    }

    public void unbind() {
        if (mServiceConn != null) {
            activity.unbindService(mServiceConn);
            mServiceConn = null;
        }
    }

    public interface ManagerListener {
        public void onDataUpdated(ArrayList<PurchaseItem> items);

        public void onErrorUpdating(Exception e);

        public void onPurchase(PurchaseItem item);

        public void onLoadedboughtItems(ArrayList<PurchaseItem> items);

        public void onPurchaseCancel();

        public void onConsume(PurchaseItem item);

        public void onConsumeFail(PurchaseItem item);

        public void onPurchaseFail(Exception e);

        public void onFakeException(int amount);

        public void onConnectedToService();
    }
}
