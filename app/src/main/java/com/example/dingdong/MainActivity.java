package com.example.dingdong;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.developer.kalert.KAlertDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.poliveira.parallaxrecyclerview.ParallaxRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    boolean check = true; // FCM 관련 중복된 토큰 판별
    int count; // DB 크롤링 데이터 갯수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //DB 로딩동안 출력되는 로딩창
        final ProgressDialog Dialog = new ProgressDialog(this);
        Dialog.setMessage("데이터를 불러오는 중...");
        Dialog.show();

        // 개발자 관련 안내 플로팅 버튼
        final FloatingActionButton actionA = (FloatingActionButton) findViewById(R.id.action_a);
        actionA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://github.com/Team-Comgong/DingDong-backend"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        final FloatingActionButton actionB = (FloatingActionButton) findViewById(R.id.action_b);
        actionB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://github.com/Team-Comgong/DingDong"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        //Firebase 관련 처리
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("getInstanceId failed", task.getException());
                            return;
                        }

                        // FCM 토큰 생성
                        final String token = task.getResult().getToken();
                        Log.d("My token is : ", token);

                        // 해당 토큰이 이미 DB에 존재한다면 Pass 반대경우는 Input
                        final DatabaseReference mDatabase;// ...
                        mDatabase = FirebaseDatabase.getInstance().getReference();
                        mDatabase.child("Device_ID").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot sp : dataSnapshot.getChildren())
                                {
                                    String str = sp.getValue(String.class);
                                    if (str.equals(token))
                                    {
                                        check = false;
                                        break;
                                    }
                                    Log.d("DB FCM tokens are : ", str);
                                }
                                if (check)
                                    mDatabase.push().setValue(token);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        // DB 크롤링 데이터 갯수 만큼 리스트 출력
                        mDatabase.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                count = (int) dataSnapshot.getChildrenCount();
                                RecyclerView myRecycler = (RecyclerView) findViewById(R.id.myRecycler);
                                LinearLayoutManager manager = new LinearLayoutManager(MainActivity.this);
                                manager.setOrientation(LinearLayoutManager.VERTICAL);
                                myRecycler.setLayoutManager(manager);
                                myRecycler.setHasFixedSize(true);

                                final List<String> content = new ArrayList<>();
                                for (DataSnapshot sp : dataSnapshot.getChildren())
                                {
                                    Log.d("DB Keys are : ", String.valueOf(sp.getKey()));
                                    content.add(getListString(sp.getKey()));
                                }
                                Dialog.hide(); // DB 관련 로딩이 종료되면 로딩창 제거

                                ParallaxRecyclerAdapter<String> stringAdapter = new ParallaxRecyclerAdapter<String>(content) {
                                    @Override
                                    public void onBindViewHolderImpl(RecyclerView.ViewHolder viewHolder, ParallaxRecyclerAdapter parallaxRecyclerAdapter, int i) {
                                        ((TextView) viewHolder.itemView).setText(content.get(i));
                                    }

                                    @Override
                                    public RecyclerView.ViewHolder onCreateViewHolderImpl(ViewGroup viewGroup, ParallaxRecyclerAdapter parallaxRecyclerAdapter, int i) {
                                        return new SimpleViewHolder(getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false));
                                    }

                                    @Override
                                    public int getItemCountImpl(ParallaxRecyclerAdapter parallaxRecyclerAdapter) {
                                        return content.size();
                                    }
                                };

                                stringAdapter.setParallaxHeader(getLayoutInflater().inflate(R.layout.my_header, myRecycler, false), myRecycler);
                                stringAdapter.setOnParallaxScroll(new ParallaxRecyclerAdapter.OnParallaxScroll() {
                                    @Override
                                    public void onParallaxScroll(float percentage, float offset, View parallax) {
                                        //TODO: implement toolbar alpha. See README for details
                                    }
                                });
                                myRecycler.setAdapter(stringAdapter);

                                // 리스트 아이템 터치 이벤트
                                stringAdapter.setOnClickEvent(new ParallaxRecyclerAdapter.OnClickEvent() {
                                    @Override
                                    public void onClick(View view, int i) {
                                        new KAlertDialog(MainActivity.this, KAlertDialog.SUCCESS_TYPE) // 팝업창 생성
                                                .setTitleText("선택한 인덱스는")
                                                .setContentText(i+"입니다")
                                                .show();
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });
    }

    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }
    public String getListString(String position) {
        return position;
    }
}
