package jp.co.koenn.sixdofcontroller.fragment;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.izumin.android.bletia.BletiaException;
import jp.co.koenn.sixdofcontroller.MainActivity;
import jp.co.koenn.sixdofcontroller.R;
import jp.co.koenn.sixdofcontroller.core.BaseFragment;

import static java.lang.Math.atan;
import static java.lang.Math.max;
import static java.lang.Math.toDegrees;

/**
 * Created by tsuji on 2016/07/11.
 */
public class MainFragment extends BaseFragment implements SensorEventListener {
    final static String TAG = MainFragment.class.getSimpleName();

    @Bind(R.id.x_axis)
    TextView xAxisText;
    @Bind(R.id.y_axis)
    TextView yAxisText;
    @Bind(R.id.z_axis)
    TextView zAxisText;
    @Bind(R.id.rolling)
    TextView rollingText;
    @Bind(R.id.pitch)
    TextView pitchText;
    @Bind(R.id.yawing)
    TextView yawingText;
    @Bind(R.id.frame)
    FrameLayout frame;
    @Bind(R.id.origin)
    Button origin;
    @Bind(R.id.button)
    Button button;
    @Bind(R.id.konashi_serial)
    TextView konashi_serial;

    private static final int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_THRESHOLD_VELOCITY = 1;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private KonashiManager mKonashiManager;

    private Timer mTimer = new Timer();

    private int mWidth;
    private int mHeight;
    private int mRadius;


    private double xAxisAccValue = 0.0;
    private double yAxisAccValue = 0.0;
    private double zAxisAccValue = 0.0;
    private double xAxisSlopeValue = 0.0;
    private double yAxisSlopeValue = 0.0;
    private double zAxisSlopeValue = 0.0;

    private float xAxisDispValue = 0;
    private float yAxisDispValue = 0;
    private float zAxisDispValue = 0;
    private float alphaDispValue = 0;
    private float betaDispValue = 0;
    private float gammaDispValue = 0;

    private float scaleFactor = 1.0F;
    private float minFactor = 0.75F;
    private float maxFactor = 1.5F;

    private boolean test;
    private boolean connect = false;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, view);

        ((MainActivity)getBaseActivity()).setToolbarVisibility(View.GONE);
        mSensorManager = (SensorManager)((MainActivity)getBaseActivity()).getSystemService(getBaseActivity().SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mRadius = 200;

        frame.addView(new Rectangle(getBaseActivity()));

        button.setText(R.string.find_konashi);
        konashi_serial.setText(R.string.discon);

        ViewTreeObserver mObserver = frame.getViewTreeObserver();
        mObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){

            @Override
            public void onGlobalLayout() {
                mWidth = frame.getWidth();
                mHeight = frame.getHeight();
            }
        });

        mKonashiManager = new KonashiManager(getBaseActivity());

        mKonashiManager.addListener(new KonashiListener() {
                                        @Override
                                        public void onConnect(KonashiManager manager) {
                                            button.setText(R.string.disconnect_konashi);
                                            konashi_serial.setText(mKonashiManager.getPeripheralName());
                                            mKonashiManager.pinMode(Konashi.LED2, Konashi.OUTPUT)
                                                .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                                        @Override
                                                    public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                                        return mKonashiManager.digitalWrite(Konashi.LED2,Konashi.HIGH);
                                                    }
                                                })
                                                .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                                    @Override
                                                    public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                                        return mKonashiManager.uartMode(Konashi.UART_ENABLE);
                                                    }
                                                })
                                                .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                                    @Override
                                                    public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                                        return mKonashiManager.uartBaudrate(Konashi.UART_RATE_9K6);
                                                    }
                                                })
                                                .fail(new FailCallback<BletiaException> (){

                                                    @Override
                                                    public void onFail(BletiaException result) {
                                                        Toast.makeText(getBaseActivity(), result.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        }

                                        @Override
                                        public void onDisconnect(KonashiManager manager) {
                                            button.setText(R.string.find_konashi);
                                            konashi_serial.setText(R.string.discon);
                                        }

                                        @Override
                                        public void onError(KonashiManager manager, BletiaException e) {

                                        }

                                        @Override
                                        public void onUpdatePioOutput(KonashiManager manager, int value) {

                                        }

                                        @Override
                                        public void onUpdateUartRx(KonashiManager manager, byte[] value) {

                                        }

                                        @Override
                                        public void onUpdateSpiMiso(KonashiManager manager, byte[] value) {

                                        }

                                        @Override
                                        public void onUpdateBatteryLevel(KonashiManager manager, int level) {

                                        }
                                    });

                view.setFocusableInTouchMode(true);
        view.requestFocus();

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                calcurateSlope();
                getBaseActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rollingText.setText(getString(R.string.rolling) + " " + String.format("%02.02f",alphaDispValue));
                        pitchText.setText(getString(R.string.pitch) + " " + String.format("%02.02f",betaDispValue));
                        yawingText.setText(getString(R.string.yawing) + " " + String.format("%02.08f",zAxisDispValue));
                    }
                });
                if(mKonashiManager.isReady()){
                    mKonashiManager.uartWrite(String.format("%d",(int)xAxisDispValue) + ",")
                            .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                @Override
                                public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                    return mKonashiManager.uartWrite(String.format("%d",(int)yAxisDispValue) + ",");
                                }
                            })
                            .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                @Override
                                public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                    return mKonashiManager.uartWrite(String.format("%d",(int)zAxisDispValue) + ",");
                                }
                            })
                            .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                @Override
                                public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                    return mKonashiManager.uartWrite(String.format("%02.02f",alphaDispValue) + ",");
                                }
                            })
                            .then(new DonePipe<BluetoothGattCharacteristic, BluetoothGattCharacteristic, BletiaException, Void>() {
                                @Override
                                public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(BluetoothGattCharacteristic result) {
                                    return mKonashiManager.uartWrite(String.format("%02.02f",betaDispValue) + "\n");
                                }
                            })
                            .fail(new FailCallback<BletiaException>() {
                                @Override
                                public void onFail(BletiaException result) {
                                    Toast.makeText(getBaseActivity(), result.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        },1000,500);

        return view;
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            xAxisAccValue = (double)event.values[0];
            yAxisAccValue = (double)event.values[1];
            zAxisAccValue = (double)event.values[2];

            xAxisText.setText(getString(R.string.x_diff)+" "+String.format("%02.02f",xAxisDispValue));
            yAxisText.setText(getString(R.string.y_diff)+" "+String.format("%02.02f",yAxisDispValue));
            zAxisText.setText(getString(R.string.z_diff)+" "+String.format("%02.02f",zAxisDispValue));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void calcurateSlope(){
        xAxisSlopeValue = atan(xAxisAccValue/zAxisAccValue);
        yAxisSlopeValue = atan(yAxisAccValue/zAxisAccValue);
        if(toDegrees(xAxisSlopeValue) > 25.0){
            alphaDispValue = 25.0F;
        }else if(toDegrees(xAxisSlopeValue) < -25.0){
            alphaDispValue = -25.0F;
        }else{
            alphaDispValue = (float)toDegrees(xAxisSlopeValue);
        }
        if(toDegrees(yAxisSlopeValue) > 25.0){
            betaDispValue = 25.0F;
        }else if(toDegrees(yAxisSlopeValue) < -25.0){
            betaDispValue = -25.0F;
        }else{
            betaDispValue = (float)toDegrees(yAxisSlopeValue);
        }
    }

    public class Rectangle extends View {
        Paint paint = new Paint();
        private ScaleGestureDetector mScaleGesture;
        private boolean moveFlag = false;
        private boolean first = false;
        private float lastX;
        private float lastY;
        private int lastAction;

        public Rectangle(Context context) {
            super(context);

            mScaleGesture = new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector){
                    invalidate();
                    return super.onScaleBegin(detector);
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector){
                    scaleFactor *= detector.getScaleFactor();
                    invalidate();
                    super.onScaleEnd(detector);
                }

                @Override
                public boolean onScale(ScaleGestureDetector detector){
                    scaleFactor *= detector.getScaleFactor();
                    if(scaleFactor > maxFactor){
                        scaleFactor = maxFactor;
                    }else if(scaleFactor < minFactor){
                        scaleFactor = minFactor;
                    }
                    if(scaleFactor > 1.0){
                        zAxisDispValue = (float)(((scaleFactor - 1.0) * 28)/ 0.5);
                    }else if(scaleFactor < 1.0){
                        zAxisDispValue = (float)(((scaleFactor - 0.75) * 28)/ 0.25);
                        zAxisDispValue = -(28 - zAxisDispValue);
                    }
                    invalidate();
                    return true;
                }

            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent event){
            if(!first){
                lastAction = event.getAction();
                lastX = event.getX();
                lastY = event.getY();
                first = true;
                Log.d(TAG,"first");
                return mScaleGesture.onTouchEvent(event);
            }else{
                if(lastAction == 0 && event.getAction() == 2){
                    moveFlag = true;
                }else if(lastAction == 2 && event.getAction() == 1){
                    moveFlag = false;
                }
            }
            if(moveFlag){
                if(lastX - event.getX() > 5){
                    xAxisDispValue -= 1.0f;
                    if(xAxisDispValue < -28)xAxisDispValue = -28;
                }else if(event.getX() - lastX > 5){
                    xAxisDispValue += 1.0f;
                    if(xAxisDispValue > 28)xAxisDispValue = 28;
                }
                if(lastY - event.getY() > 5){
                    yAxisDispValue += 1.0f;
                    if(yAxisDispValue > 28)yAxisDispValue = 28;
                }else if(event.getY() - lastY > 5){
                    yAxisDispValue -= 1.0f;
                    if(yAxisDispValue < -28.0f)yAxisDispValue = -28.0f;
                }
                Log.d(TAG,"x:"+ xAxisDispValue + " y:"+ yAxisDispValue);
                lastAction = event.getAction();
                lastX = event.getX();
                lastY = event.getY();
                return true;
            }else{
                lastAction = event.getAction();
                lastX = event.getX();
                lastY = event.getY();
                return mScaleGesture.onTouchEvent(event);
            }
        }

        @Override
        public void onDraw(Canvas canvas) {
            paint.setColor(Color.GREEN);

            paint.setAntiAlias(false);
            //mRadius = 200 + (zAxisDispValue * 3);
            mRadius = (int)(200 * scaleFactor);
            canvas.drawCircle((mWidth/2) + (xAxisDispValue*3),(mHeight/2)-(yAxisDispValue*3),mRadius,paint);

            invalidate();
        }
    }

    @OnClick(R.id.origin)
    public void toOrigin(){
        xAxisDispValue = 0;
        yAxisDispValue = 0;
        scaleFactor = 1.0F;
    }

    @OnClick(R.id.button)
    public void konashiFind(){
        if(mKonashiManager.isConnected()){
            mKonashiManager.digitalWrite(Konashi.LED2,Konashi.LOW)
                    .done(new DoneCallback<BluetoothGattCharacteristic>() {
                        @Override
                        public void onDone(BluetoothGattCharacteristic result) {
                            mKonashiManager.disconnect();
                        }
                    })
                    .fail(new FailCallback<BletiaException>() {
                        @Override
                        public void onFail(BletiaException result) {
                            Toast.makeText(getBaseActivity(),result.getMessage(),Toast.LENGTH_SHORT);
                        }
                    });
        }else {
            mKonashiManager.find(getBaseActivity());
        }
    }
}
