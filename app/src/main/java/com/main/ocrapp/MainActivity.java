package com.main.ocrapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.aip.ocr.AipOcr;
import com.bumptech.glide.Glide;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public String ip="112.74.89.58";
    public int port=41027;
    public String outFile =Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/ocrapp/.image/";
    public File takePhotoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/ocrapp/.image/"+System.currentTimeMillis() + ".jpg");
    public Socket socket;
    private static final int UPLOADCOMPLETED = 0;
    private static final int ANALYSISCOMPLETED = 1;
    private static final int CONNERROR = 2;
    public static final String APP_ID = "15093641";
    public static final String API_KEY = "WCV9kiiNt56CY7ViOMlr0GZy";
    public static final String SECRET_KEY = "mBOGT8HBqi7nrumDntENhsRX1tGGSURx";
    public static final AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
    public AlertDialog ad;
    public String targetLanguage="中文";
    public String dataSource="百度Api";
    public List<list_item> historyData=new ArrayList<list_item>();
    public List<list_item> markData=new ArrayList<list_item>();
    public String histCoverFilePath="";
    public String markContent="";
    public long mExitTime=0;
    public Handler handler =new Handler(){
        @Override
        public  void handleMessage(Message msg){
            switch(msg.what){
                case UPLOADCOMPLETED:
                    ad.dismiss();
                    Toast.makeText(MainActivity.this,"上传成功",Toast.LENGTH_SHORT).show();
                    Runnable runnable2=new Runnable() {
                        @Override
                        public void run() {
                            recvData();
                        }
                    };
                    new Thread(runnable2).start();
                    break;
                case ANALYSISCOMPLETED:
                    ad.dismiss();
                    Toast.makeText(MainActivity.this,"接收数据成功",Toast.LENGTH_SHORT).show();
                    String data=msg.getData().getString("data");
                    List<Object>d=JSONObject.parseArray(data);
                    String lines="    ";
                    for(Object item:d){
                        JSONObject jo=JSONObject.parseObject(item.toString());
                        lines+=jo.getString("words")+"\n     ";
                    }
                    EditText tv=findViewById(R.id.editText);
                    tv.setText(lines);
                    markContent=lines;
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if(!histCoverFilePath.equals("")){
                        list_item li=new list_item(histCoverFilePath,lines,df.format(System.currentTimeMillis()));
                        historyData.add(0,li);
                        writeConfigFile();
                    }
                    CardView cv=findViewById(R.id.showCard);
                    cv.setVisibility(View.VISIBLE);
                    ImageView iv=findViewById(R.id.markBtn);
                    iv.setTag("mark");
                    iv.setBackgroundResource(R.drawable.mark);
                    break;
                case CONNERROR:
                    ad.dismiss();
                    Toast.makeText(MainActivity.this,"连接错误",Toast.LENGTH_SHORT).show();
                    break;
            }
            return;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        readConfigFile();
        Button btn=findViewById(R.id.submit);//提交按钮
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ab=new AlertDialog.Builder(MainActivity.this);
                View view =View.inflate(MainActivity.this, R.layout.loading, null);
                ab.setView(view);
                ad=ab.create();
                ad.setCancelable(false);
                ad.show();
                try {
                    ImageView iv3=findViewById(R.id.imageView3);
                    iv3.setDrawingCacheEnabled(true);
                    Bitmap bt = Bitmap.createBitmap(iv3.getDrawingCache());
                    iv3.setDrawingCacheEnabled(false);
                    final File file=Bitmap2File(bt);
                    histCoverFilePath=file.getAbsolutePath();
                    final byte[] byteArray=new byte[(int)file.length()];
                    FileInputStream fis=new FileInputStream(file);
                    fis.read(byteArray);
                    fis.close();
                   Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if(dataSource.equals("opencv")) uploadFile(file);
                            else if(dataSource.equals("百度Api")) baiduAPi(byteArray);
                        }
                    };
                    new Thread(runnable).start();
                }catch(Exception e){
                }
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View cv=findViewById(R.id.cv1);
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it=new Intent(Intent.ACTION_GET_CONTENT);
                it.setType("image/*");
                startActivityForResult(it,2);
            }
        });

        ImageView iv3=findViewById(R.id.imageView3);
        iv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setDrawingCacheEnabled(true);
                Bitmap bt=Bitmap.createBitmap(v.getDrawingCache());
                v.setDrawingCacheEnabled(false);
                Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bt, null,null));
                Uri destinationUri = Uri.fromFile(takePhotoFile);
                UCrop.Options options=new UCrop.Options();
                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
                //设置toolbar颜色
                options.setToolbarColor(ActivityCompat.getColor(MainActivity.this, R.color.colorPrimaryDark));
                //设置状态栏颜色
                options.setStatusBarColor(ActivityCompat.getColor(MainActivity.this, R.color.colorPrimaryDark));
                //是否能调整裁剪框
                options.setFreeStyleCropEnabled(true);
                UCrop ucrop= UCrop.of(uri,destinationUri);
                ucrop.withOptions(options);
                ucrop.start(MainActivity.this);
            }
        });
        iv3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent it=new Intent(Intent.ACTION_GET_CONTENT);
                it.setType("image/*");
                startActivityForResult(it,2);
                return false;
            }
        });
        setViewInit();
        spinnerEvent();
        MarkBtnEvent();

        Button selectPhotoBtn=findViewById(R.id.selectPhotoBtn);
        final Button takePhotoBtn=findViewById(R.id.takePhotoBtn);
        selectPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it=new Intent(Intent.ACTION_GET_CONTENT);
                it.setType("image/*");
                startActivityForResult(it,2);
            }
        });
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it=new Intent("android.media.action.IMAGE_CAPTURE");
                takePhotoFile.getParentFile().mkdirs();
                Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.xykj.customview.fileprovider", takePhotoFile);
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加权限
                it.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(it,1);
            }
        });

    }
    public void MarkBtnEvent(){
        final ImageView iv=findViewById(R.id.markBtn);
        iv.setTag("mark");
        iv.setBackgroundResource(R.drawable.mark);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //Toast.makeText(MainActivity.this,v.getTag().toString(),Toast.LENGTH_SHORT).show();
                if(v.getTag().toString().equals("mark")){//收藏
                    v.setBackgroundResource(R.drawable.marked);
                    v.setTag("marked");
                    DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    list_item li=new list_item(histCoverFilePath,markContent,df.format(System.currentTimeMillis()));
                    if(markData==null)markData=new ArrayList<>();
                    markData.add(0,li);
                }else{//取消收藏
                    v.setBackgroundResource(R.drawable.mark);
                    v.setTag("mark");
                    markData.remove(0);
                }
                writeConfigFile();

            }
        });
    }
    public void spinnerEvent(){
        final Spinner sp1=findViewById(R.id.targetSpinner);
        final Spinner sp2=findViewById(R.id.sourceSpinner);
        sp1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetLanguage=sp1.getItemAtPosition(position).toString();
                writeConfigFile();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataSource=sp2.getItemAtPosition(position).toString();
                writeConfigFile();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }
    public void setViewInit(){
        final EditText et1=findViewById(R.id.editPort);
        et1.setText(port+"");
        final EditText et2=findViewById(R.id.editIP);
        et2.setText(ip);
        Button btn2=findViewById(R.id.button);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((Button)v).getText()=="修改") {
                    v.setBackgroundColor(Color.argb(255, 33, 150, 243));
                    ((Button) v).setText("确定");
                    et1.setEnabled(true);
                    et2.setEnabled(true);
                }
                else{
                    ip=et2.getText().toString();
                    try {
                        port = Integer.parseInt(et1.getText().toString());
                        v.setBackgroundColor(Color.parseColor("#ffd6d7d7"));
                        ((Button) v).setText("修改");
                        et1.setEnabled(false);
                        et2.setEnabled(false);
                        writeConfigFile();
                    }catch(Exception e){
                        Toast.makeText(MainActivity.this,"请输入数字",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    public void histroyViewInit(){
        final FloatingActionButton fab=findViewById(R.id.floatingActionButton2);
        final List<Integer>selectItem=new ArrayList<>();
        final ListView lv=findViewById(R.id.historyList);
        final listAdapter la=new listAdapter(MainActivity.this,historyData);
        final TextView selectNum=findViewById(R.id.selectNum);
        final Button selectAll=findViewById(R.id.selectAll);
        selectAll.setTag("selectAll");
        if(historyData!=null){
            lv.setAdapter(la);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(!la.isChoice) {
                        View camera_main = findViewById(R.id.include1);
                        View history_main = findViewById(R.id.include_history);
                        Toolbar tb=findViewById(R.id.toolbar);
                        tb.setTitle("历史记录(详情)");
                        camera_main.setVisibility(View.VISIBLE);
                        camera_main.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_right));
                        history_main.setVisibility(View.GONE);
                        history_main.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_left));
                        ImageView iv = findViewById(R.id.imageView3);//显示图片的imageview
                        iv.setVisibility(View.VISIBLE);
                        File file = new File(historyData.get(position).getCover());
                        histCoverFilePath = historyData.get(position).getCover();
                        markContent = historyData.get(position).getContent();
                        Glide.with(MainActivity.this).load(file).into(iv);
                        EditText et = findViewById(R.id.editText);//显示结果的文本框
                        View cv = findViewById(R.id.showCard);//显示结果的Card
                        cv.setVisibility(View.VISIBLE);
                        et.setText(historyData.get(position).getContent());
                        ImageView markiv = findViewById(R.id.markBtn);//收藏按钮
                        markiv.setTag("mark");
                        markiv.setBackgroundResource(R.drawable.mark);
                    }else{
                        View item= lv.getChildAt(position);
                        if(item.getTag().equals("selected")){
                            item.setTag("select");
                            item.setBackgroundColor(Color.WHITE);
                            for(int i=0;i<selectItem.size();i++){
                                if(selectItem.get(i)==position){
                                    selectItem.remove(i);
                                    break;
                                }
                            }
                        }else{
                            item.setTag("selected");
                            item.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                            selectItem.add(position);
                        }
                        if(selectItem.size()==0){
                            la.isChoice=false;
                            fab.setVisibility(View.GONE);
                            selectAll.setVisibility(View.GONE);
                            selectNum.setVisibility(View.GONE);
                        }
                        selectNum.setText(selectItem.size()+"");
                    }
                }
            });
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    View item= lv.getChildAt(position);
                    item.setTag("selected");
                    la.isChoice=true;
                    item.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    selectItem.add(position);
                    fab.setVisibility(View.VISIBLE);
                    selectAll.setVisibility(View.VISIBLE);
                    selectNum.setVisibility(View.VISIBLE);
                    selectNum.setText(selectItem.size()+"");
                    return true;
                }
            });
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectItem!=null){
                     AlertDialog.Builder ab=new AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("确认删除?");
                    ab.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for(int i : selectItem){
                                historyData.remove(i);
                            }
                              selectItem.clear();
                              la.notifyDataSetChanged();
                              writeConfigFile();
                              la.isChoice=false;
                              fab.setVisibility(View.GONE);
                              selectAll.setVisibility(View.GONE);
                              selectNum.setVisibility(View.GONE);
                        }
                    });
                    ab.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });
                    ab.create().show();
                }
            }
        });
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectAll.getTag().equals("selectAll")){
                    selectAll.setTag("selectedAll");
                    selectAll.setText("取消全选");
                    selectItem.clear();
                    for(int i=0;i<historyData.size();i++){
                        selectItem.add(i);
                        lv.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                        lv.getChildAt(i).setTag("selected");
                    }
                }else{
                    selectAll.setTag("selectAll");
                    selectAll.setText("全选");
                    selectItem.clear();
                    for(int i=0;i<historyData.size();i++){
                        lv.getChildAt(i).setBackgroundColor(Color.WHITE);
                        lv.getChildAt(i).setTag("select");
                    }
                    la.isChoice=false;
                    fab.setVisibility(View.GONE);
                    selectAll.setVisibility(View.GONE);
                    selectNum.setVisibility(View.GONE);
                }
                selectNum.setText(selectItem.size()+"");
            }
        });
    }
    public void markViewInit(){
        final FloatingActionButton fab=findViewById(R.id.mark_fab);
        final List<Integer>selectItem=new ArrayList<>();
        final ListView lv=findViewById(R.id.markList);
        final listAdapter la=new listAdapter(MainActivity.this,markData);
        final TextView selectNum=findViewById(R.id.selectNum);
        final Button selectAll=findViewById(R.id.selectAll);
        selectAll.setTag("selectAll");
        if(markData!=null) {
            lv.setAdapter(la);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(!la.isChoice) {
                        View camera_main = findViewById(R.id.include1);
                        View mark_main = findViewById(R.id.include2);
                        Toolbar tb=findViewById(R.id.toolbar);
                        tb.setTitle("收藏(详情)");
                        camera_main.setVisibility(View.VISIBLE);
                        camera_main.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_right));
                        mark_main.setVisibility(View.GONE);
                        mark_main.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_left));
                        ImageView iv = findViewById(R.id.imageView3);//显示图片的imageview
                        iv.setVisibility(View.VISIBLE);
                        File file = new File(markData.get(position).getCover());
                        histCoverFilePath = markData.get(position).getCover();
                        markContent = markData.get(position).getContent();
                        Glide.with(MainActivity.this).load(file).into(iv);
                        EditText et = findViewById(R.id.editText);//显示结果的文本框
                        View cv = findViewById(R.id.showCard);//显示结果的Card
                        cv.setVisibility(View.VISIBLE);
                        et.setText(markData.get(position).getContent());
                        ImageView markiv = findViewById(R.id.markBtn);//收藏按钮
                        markiv.setTag("mark");
                        markiv.setBackgroundResource(R.drawable.mark);
                    }else{
                        View item= lv.getChildAt(position);
                        if(item.getTag().equals("selected")){
                            item.setTag("select");
                            item.setBackgroundColor(Color.WHITE);
                            for(int i=0;i<selectItem.size();i++){
                                if(selectItem.get(i)==position){
                                    selectItem.remove(i);
                                    break;
                                }
                            }
                        }else{
                            item.setTag("selected");
                            item.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                            selectItem.add(position);
                        }
                        if(selectItem.size()==0){
                            la.isChoice=false;
                            fab.setVisibility(View.GONE);
                            selectAll.setVisibility(View.GONE);
                            selectNum.setVisibility(View.GONE);
                        }
                        selectNum.setText(selectItem.size()+"");
                    }
                }
            });
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    View item= lv.getChildAt(position);
                    item.setTag("selected");
                    la.isChoice=true;
                    item.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    selectItem.add(position);
                    fab.setVisibility(View.VISIBLE);
                    selectAll.setVisibility(View.VISIBLE);
                    selectNum.setVisibility(View.VISIBLE);
                    selectNum.setText(selectItem.size()+"");
                    return true;
                }
            });
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectItem!=null){
                    AlertDialog.Builder ab=new AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("确认删除?");
                    ab.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for(int i : selectItem){
                                markData.remove(i);
                            }
                            selectItem.clear();
                            la.notifyDataSetChanged();
                            writeConfigFile();
                            la.isChoice=false;
                            fab.setVisibility(View.GONE);
                            selectAll.setVisibility(View.GONE);
                            selectNum.setVisibility(View.GONE);
                        }
                    });
                    ab.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });
                    ab.create().show();
                }
            }
        });
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectAll.getTag().equals("selectAll")){
                    selectAll.setTag("selectedAll");
                    selectAll.setText("取消全选");
                    selectItem.clear();
                    for(int i=0;i<markData.size();i++){
                        selectItem.add(i);
                        lv.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                        lv.getChildAt(i).setTag("selected");
                    }
                }else{
                    selectAll.setTag("selectAll");
                    selectAll.setText("全选");
                    selectItem.clear();
                    for(int i=0;i<markData.size();i++){
                        lv.getChildAt(i).setBackgroundColor(Color.WHITE);
                        lv.getChildAt(i).setTag("select");
                    }
                    la.isChoice=false;
                    fab.setVisibility(View.GONE);
                    selectAll.setVisibility(View.GONE);
                    selectNum.setVisibility(View.GONE);
                }
                selectNum.setText(selectItem.size()+"");
            }
        });

    }
    public void aboutViewInit(){
       final TextView tv=findViewById(R.id.textView6);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setData(Uri.parse(tv.getText()+""));
                intent.setAction("android.intent.action.VIEW");
                startActivity(intent);
            }
        });
    }
    public void writeConfigFile(){
        Map<String,String> var=new HashMap<>();
        var.put("ip",ip);
        var.put("port",port+"");
        var.put("targetLanguage",targetLanguage);
        var.put("dataSource",dataSource);
        String history_list2string=JSON.toJSONString(historyData);
        var.put("historyData",history_list2string);
        String mark_list2string=JSON.toJSONString(markData);
        var.put("markData",mark_list2string);
        JSONObject var_map2json= JSONObject.parseObject(JSON.toJSONString(var));
        File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/ocrapp/config.json");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bw=new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
            bw.write(var_map2json.toJSONString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void readConfigFile(){
        File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/ocrapp/config.json");
        if(file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile())));
                String data="";
                String line=br.readLine();
                while (line!=null) {
                    data=data+line;
                    line=br.readLine();
                }
                br.close();
                JSONObject jo = JSONObject.parseObject(data);
                ip = jo.getString("ip");
                port = Integer.parseInt(jo.getString("port"));
                targetLanguage=jo.getString("targetLanguage");
                dataSource=jo.getString("dataSource");
                historyData=JSONObject.parseArray(jo.getString("historyData"),list_item.class);
                markData=JSONObject.parseArray(jo.getString("markData"),list_item.class);

                Spinner sp1=findViewById(R.id.targetSpinner);
                SpinnerAdapter sa1=sp1.getAdapter();
                Spinner sp2=findViewById(R.id.sourceSpinner);
                SpinnerAdapter sa2=sp2.getAdapter();
                for(int i=0;i< sa1.getCount();i++){
                    if(targetLanguage.equals(sa1.getItem(i).toString())){
                        sp1.setSelection(i,true);
                    }
                }
                for(int i=0;i< sa2.getCount();i++){
                    if(dataSource.equals(sa2.getItem(i).toString())){
                        sp2.setSelection(i,true);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            ImageView iv=findViewById(R.id.imageView3);
            File ucropOutFile=new File(outFile+System.currentTimeMillis() + ".jpg");
            ucropOutFile.getParentFile().mkdirs();
            Uri destinationUri = Uri.fromFile(ucropOutFile);
            UCrop.Options options=new UCrop.Options();
            options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
            options.setToolbarColor(ActivityCompat.getColor(MainActivity.this, R.color.colorPrimaryDark));
            options.setStatusBarColor(ActivityCompat.getColor(MainActivity.this, R.color.colorPrimaryDark));
            options.setFreeStyleCropEnabled(true);
            if(requestCode==1){//相机返回
                try {
                    File compressorFile=new Compressor(this).compressToFile(takePhotoFile);
                    Uri uri=getImageContentUri(MainActivity.this,compressorFile);
                    UCrop ucrop= UCrop.of(uri,destinationUri);
                    ucrop.withOptions(options);
                    ucrop.start(MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(requestCode==2){//图库返回
                Uri uri=data.getData();
                    UCrop ucrop= UCrop.of(uri,destinationUri);
                    ucrop.withOptions(options);
                    ucrop.start(MainActivity.this);
            }
            else if(requestCode==UCrop.REQUEST_CROP){//剪裁返回
                    Uri uri=UCrop.getOutput(data);
                   // Bitmap bm = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), uri);
                    iv.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(iv);
            }

        }
    }
    public static Uri getImageContentUri(Context context, java.io.File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Toolbar tb=findViewById(R.id.toolbar);
            if(tb.getTitle().equals("历史记录(详情)")){
                View home=findViewById(R.id.include1);
                View history=findViewById(R.id.include_history);
                home.setVisibility(View.GONE);
                home.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_right));
                history.setVisibility(View.VISIBLE);
                history.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_left));
                tb.setTitle("历史记录");
            }
            else if(tb.getTitle().equals("收藏(详情)")){
                View home=findViewById(R.id.include1);
                View mark=findViewById(R.id.include2);
                home.setVisibility(View.GONE);
                home.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_out_right));
                mark.setVisibility(View.VISIBLE);
                mark.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_left));
                tb.setTitle("收藏");
            }
            else{
                if(System.currentTimeMillis() - mExitTime > 2000) {
                    mExitTime = System.currentTimeMillis();
                    Toast.makeText(this, "再次点击退出", Toast.LENGTH_SHORT).show();
                } else {
                    super.onBackPressed();
                }
            }

        }
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Toolbar tb=findViewById(R.id.toolbar);
        List<View> views=new ArrayList<>();
        views.add(findViewById(R.id.include1));
        views.add(findViewById(R.id.include2));
        views.add(findViewById(R.id.include_set));
        views.add(findViewById(R.id.include_history));
        views.add(findViewById(R.id.include_about));
        int id = item.getItemId();
        if (id == R.id.nav_camera) {
           switch_view(views,findViewById(R.id.include1));
           tb.setTitle("首页");
        } else if (id == R.id.nav_mark) {
            switch_view(views,findViewById(R.id.include2));
            tb.setTitle("收藏");
            markViewInit();
        } else if (id == R.id.nav_history) {
            switch_view(views,findViewById(R.id.include_history));
            tb.setTitle("历史记录");
            histroyViewInit();
        } else if (id == R.id.nav_set) {
            tb.setTitle("设置");
            switch_view(views,findViewById(R.id.include_set));
        } else if (id == R.id.nav_about) {
            tb.setTitle("关于");
            switch_view(views,findViewById(R.id.include_about));
            aboutViewInit();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    void switch_view(List<View>views,View actView){
        actView.setVisibility(View.VISIBLE);
        actView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.slide_in_top));
        for(int i=0;i<views.size();i++){
            if(views.get(i)!=actView){
                views.get(i).setVisibility(View.GONE);
            }
        }
    }
    protected  void uploadFile(File file){
        try {
            socket=new Socket(ip,port);
        } catch (IOException e) {
            e.printStackTrace();
            Message msg=new Message();
            msg.what=CONNERROR;
            handler.sendMessage(msg);
            return ;
        }
        if(file!=null){
            try {
                File compressorFile=new Compressor(this).compressToFile(file);
                InputStream is=new FileInputStream(compressorFile);
                OutputStream os=socket.getOutputStream();
                byte buffer[]=new byte[1024];
                int temp=0;
                while((temp=is.read(buffer))>0){
                    os.write(buffer,0,temp);
                }
                os.flush();
                is.close();
                socket.shutdownOutput();
                Message msg = new Message();
                msg.what = UPLOADCOMPLETED;
                handler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
              }

        }
    }
    public void recvData(){
        try {
            socket=new Socket(ip,port);
            Message msg=new Message();
            while(true) {
                BufferedReader br = null;
                br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                String line = br.readLine();
                if (line != null) {
                    msg.what = ANALYSISCOMPLETED;
                    Bundle bundle = new Bundle();
                    bundle.putString("data", line);
                    br.close();
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Message msg=new Message();
            msg.what=CONNERROR;
            handler.sendMessage(msg);
         }
    }
    public void baiduAPi(byte[] byteArray){
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("probability", "false");
        try {
            org.json.JSONObject jsn=client.basicGeneral(byteArray,options);
            Message msg = new Message();
            msg.what =ANALYSISCOMPLETED;
            Bundle bl=new Bundle();
            bl.putString("data",jsn.getString("words_result"));
            msg.setData(bl);
            handler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
            Message msg=new Message();
            msg.what=CONNERROR;
            handler.sendMessage(msg);
            return ;
        }

    }
    public File Bitmap2File(Bitmap bitmap) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
               + "/ocrapp/.image/" + System.currentTimeMillis() + ".jpg");//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
