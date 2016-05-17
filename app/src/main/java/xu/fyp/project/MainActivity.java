package xu.fyp.project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends ActionBarActivity {
    private ListView item_list;
    private TextView show_app_name;

    // ListView使用的自定Adapter物件
    private ItemAdapter itemAdapter;
    // 儲存所有記事本的List物件
    private List<Item> items;

    // 選單項目物件
    private MenuItem add_item, search_item, revert_item, share_item, delete_item;

    // 已選擇項目數量
    private int selectedCount = 0;

    // 宣告資料庫功能類別欄位變數
    private ItemDAO itemDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        processViews();
        processControllers();

        // 建立資料庫物件
        itemDAO = new ItemDAO(getApplicationContext());

        // 如果資料庫是空的，就建立一些範例資料
        // 這是為了方便測試用的，完成應用程式以後可以拿掉


        // 取得所有記事資料
        items = itemDAO.getAll();

        itemAdapter = new ItemAdapter(this, R.layout.single_item, items);
        item_list.setAdapter(itemAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Item item = (Item) data.getExtras().getSerializable(
                    "xu.fyp.project.Item");

            if (requestCode == 0) {
                // 新增記事資料到資料庫

                item = itemDAO.insert(item);

                items.add(item);

                itemAdapter.notifyDataSetChanged();
            }
            else if (requestCode == 1) {
                int position = data.getIntExtra("position", -1);

                if (position != -1) {
                    // 修改資料庫中的記事資料
                    itemDAO.update(item);

                    items.set(position, item);
                    itemAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void processViews() {
        item_list = (ListView)findViewById(R.id.item_list);
        show_app_name = (TextView) findViewById(R.id.show_app_name);
    }

    private void processControllers() {

        // 建立選單項目點擊監聽物件
        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // 讀取選擇的記事物件
                Item item = itemAdapter.getItem(position);

                // 如果已經有勾選的項目
                if (selectedCount > 0) {
                    // 處理是否顯示已選擇項目
                    processMenu(item);
                    // 重新設定記事項目
                    itemAdapter.set(position, item);
                } else {
                    Intent intent = new Intent(
                            MainActivity.this, PlayActivity.class);

                    // 設定記事編號與記事物件
                    intent.putExtra("recFileName", item.getRecFileName());
                    //Log.e("ss", item.getFileName());
                    intent.putExtra("fileName",item.getFileName());
                    intent.putExtra("xu.fyp.project.Item", item);

                    startActivityForResult(intent, 1);
                }
            }
        };

        // 註冊選單項目點擊監聽物件
        item_list.setOnItemClickListener(itemListener);

        // 建立記事項目長按監聽物件
        AdapterView.OnItemLongClickListener itemLongListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                // 讀取選擇的記事物件
                Item item = itemAdapter.getItem(position);
                // 處理是否顯示已選擇項目
                processMenu(item);
                // 重新設定記事項目
                itemAdapter.set(position, item);
                return true;
            }
        };

        // 註冊記事項目長按監聽物件
        item_list.setOnItemLongClickListener(itemLongListener);

    }

    // 處理是否顯示已選擇項目
    private void processMenu(Item item) {
        // 如果需要設定記事項目
        if (item != null) {
            // 設定已勾選的狀態
            item.setSelected(!item.isSelected());

            // 計算已勾選數量
            if (item.isSelected()) {
                selectedCount++;
            }
            else {
                selectedCount--;
            }
        }

        // 根據選擇的狀況，設定是否顯示選單項目
        add_item.setVisible(selectedCount == 0);

        revert_item.setVisible(selectedCount > 0);
        share_item.setVisible(selectedCount > 0);
        delete_item.setVisible(selectedCount > 0);
    }

    // 載入選單資源
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        // 取得選單項目物件
        add_item = menu.findItem(R.id.add_item);

        revert_item = menu.findItem(R.id.revert_item);
        share_item = menu.findItem(R.id.share_item);
        delete_item = menu.findItem(R.id.delete_item);

        // 設定選單項目
        processMenu(null);

        return true;
    }

    // 使用者選擇所有的選單項目都會呼叫這個方法
    public void clickMenuItem(MenuItem item) {
        // 使用參數取得使用者選擇的選單項目元件編號
        int itemId = item.getItemId();

        // 判斷該執行什麼工作，目前還沒有加入需要執行的工作
        switch (itemId) {

            case R.id.add_item:
                // 使用Action名稱建立啟動另一個Activity元件需要的Intent物件

                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                // 呼叫「startActivityForResult」，，第二個參數「0」表示執行新增
                startActivityForResult(intent, 0);

                break;
            // 取消所有已勾選的項目
            case R.id.revert_item:
                for (int i = 0; i < itemAdapter.getCount(); i++) {
                    Item ri = itemAdapter.getItem(i);

                    if (ri.isSelected()) {
                        ri.setSelected(false);
                        itemAdapter.set(i, ri);
                    }
                }

                selectedCount = 0;
                processMenu(null);

                break;
            // 刪除
            case R.id.delete_item:
                if (selectedCount == 0) {
                    break;
                }

                AlertDialog.Builder d = new AlertDialog.Builder(this);
                String message = getString(R.string.delete_item);
                d.setTitle(R.string.delete)
                        .setMessage(String.format(message, selectedCount));
                d.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 取得最後一個元素的編號
                                int index = itemAdapter.getCount() - 1;

                                while (index > -1) {
                                    Item item = itemAdapter.get(index);

                                    if (item.isSelected()) {
                                        itemAdapter.remove(item);
                                        // 刪除資料庫中的記事資料
                                        itemDAO.delete(item.getId());
                                    }

                                    index--;
                                }

                                itemAdapter.notifyDataSetChanged();
                                selectedCount = 0;
                                processMenu(null);
                            }
                        });
                d.setNegativeButton(android.R.string.no, null);
                d.show();

                break;
            case R.id.googleplus_item:
                break;
            case R.id.facebook_item:
                break;
        }

    }

    // 方法名稱與onClick的設定一樣，參數的型態是android.view.View
    public void aboutApp(View view) {
        Intent intent = new Intent(MainActivity.this, RecordActivity.class);
        // 呼叫「startActivityForResult」，，第二個參數「0」表示執行新增
        startActivityForResult(intent, 0);
    }

    // 設定
    public void clickPreferences(MenuItem item) {
        // 啟動設定元件
        startActivity(new Intent(this, PrefActivity.class));
    }

}


