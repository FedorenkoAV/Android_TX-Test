package com.example.user.txtest;

import android.content.Context;


import android.graphics.Color;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;

import android.view.Menu;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    // j2xx
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1; //Это число подключенных портов
    int currentPortIndex = -1;// Это номер порта который открыт
    int portIndex = -1; // Это номер порта который отображается в спиннере portsList


    enum DeviceStatus {
        DEV_NOT_CONNECT,
        DEV_NOT_CONFIG,
        DEV_CONFIG
    }

    boolean INTERNAL_DEBUG_TRACE = false; // Toast message for debug

    // menu item
    Menu myMenu;
    final int MENU_CONTENT_FORMAT = Menu.FIRST;
    final int MENU_FONT_SIZE = Menu.FIRST + 1;
    final int MENU_SAVE_CONTENT_DATA = Menu.FIRST + 2;
    final int MENU_CLEAN_SCREEN = Menu.FIRST + 3;
    final int MENU_ECHO = Menu.FIRST + 4;
    final int MENU_HELP = Menu.FIRST + 5;
    final int MENU_SETTING = Menu.FIRST + 6;

    final String[] contentFormatItems = {"Character", "Hexadecimal"};
    final String[] fontSizeItems = {"5", "6", "7", "8", "10", "12", "14", "16", "18", "20"};
    final String[] echoSettingItems = {"On", "Off"};

    // log tag
    final String TT = "Trace";
    final String TXS = "XM-Send";
    final String TXR = "XM-Rec";
    final String TYS = "YM-Send";
    final String TYR = "YM-Rec";
    final String TZS = "ZM-Send";
    final String TZR = "ZM-Rec";

    // handler event
    final int UPDATE_TEXT_VIEW_CONTENT = 0;
    final int UPDATE_SEND_FILE_STATUS = 1;
    final int UPDATE_SEND_FILE_DONE = 2;
    final int ACT_SELECT_SAVED_FILE_NAME = 3;
    final int ACT_SELECT_SAVED_FILE_FOLDER = 4;
    final int ACT_SAVED_FILE_NAME_CREATED = 5;
    final int ACT_SELECT_SEND_FILE_NAME = 6;
    final int MSG_SELECT_FOLDER_NOT_FILE = 7;
    final int MSG_XMODEM_SEND_FILE_TIMEOUT = 8;
    final int UPDATE_MODEM_RECEIVE_DATA = 9;
    final int UPDATE_MODEM_RECEIVE_DATA_BYTES = 10;
    final int UPDATE_MODEM_RECEIVE_DONE = 11;
    final int MSG_MODEM_RECEIVE_PACKET_TIMEOUT = 12;
    final int ACT_MODEM_SELECT_SAVED_FILE_FOLDER = 13;
    final int MSG_MODEM_OPEN_SAVE_FILE_FAIL = 14;
    final int MSG_YMODEM_PARSE_FIRST_PACKET_FAIL = 15;
    final int MSG_FORCE_STOP_SEND_FILE = 16;
    final int UPDATE_ASCII_RECEIVE_DATA_BYTES = 17;
    final int UPDATE_ASCII_RECEIVE_DATA_DONE = 18;
    final int MSG_FORCE_STOP_SAVE_TO_FILE = 19;
    final int UPDATE_ZMODEM_STATE_INFO = 20;
    final int ACT_ZMODEM_AUTO_START_RECEIVE = 21;

    final int MSG_SPECIAL_INFO = 98;
    final int MSG_UNHANDLED_CASE = 99;

    final byte XON = 0x11;    /* Resume transmission */
    final byte XOFF = 0x13;    /* Pause transmission */

    // strings of file transfer protocols
    final String[] protocolItems = {"ASCII", "XModem-CheckSum", "XModem-CRC", "XModem-1KCRC", "YModem", "ZModem"};
    String currentProtocol;

    final int MODE_GENERAL_UART = 0;
    final int MODE_X_MODEM_CHECKSUM_RECEIVE = 1;
    final int MODE_X_MODEM_CHECKSUM_SEND = 2;
    final int MODE_X_MODEM_CRC_RECEIVE = 3;
    final int MODE_X_MODEM_CRC_SEND = 4;
    final int MODE_X_MODEM_1K_CRC_RECEIVE = 5;
    final int MODE_X_MODEM_1K_CRC_SEND = 6;
    final int MODE_Y_MODEM_1K_CRC_RECEIVE = 7;
    final int MODE_Y_MODEM_1K_CRC_SEND = 8;
    final int MODE_Z_MODEM_RECEIVE = 9;
    final int MODE_Z_MODEM_SEND = 10;
    final int MODE_SAVE_CONTENT_DATA = 11;

    int transferMode = MODE_GENERAL_UART;
    int tempTransferMode = MODE_GENERAL_UART;

    // X, Y, Z modem - UART MODE: Asynchronous 8 data bits no parity one stop bit
    // X modem + //
    final int PACTET_SIZE_XMODEM_CHECKSUM = 132; // SOH,pkt,~ptk,128data,checksum
    final int PACTET_SIZE_XMODEM_CRC = 133;     // SOH,pkt,~ptk,128data,CRC-H,CRC-L
    final int PACTET_SIZE_XMODEM_1K_CRC = 1029;     // STX,pkt,~ptk,1024data,CRC-H,CRC-L

    final byte SOH = 1;    /* Start Of Header */
    final byte STX = 2;    /* Start Of Header 1K */
    final byte EOT = 4;    /* End Of Transmission */
    final byte ACK = 6;    /* ACKnowlege */
    final byte NAK = 0x15; /* Negative AcKnowlege */
    final byte CAN = 0x18; /* Cancel */
    final byte CHAR_C = 0x43; /* Character 'C' */
    final byte CHAR_G = 0x47; /* Character 'G' */

    final int DATA_SIZE_128 = 128;
    final int DATA_SIZE_256 = 256;
    final int DATA_SIZE_512 = 512;
    final int DATA_SIZE_1K = 1024;

    final int MODEM_BUFFER_SIZE = 2048;
    int[] modemReceiveDataBytes;
    byte[] modemDataBuffer;
    byte[] zmDataBuffer;
    byte receivedPacketNumber = 1;

    boolean bModemGetNak = false;
    boolean bModemGetAck = false;
    boolean bModemGetCharC = false;
    boolean bModemGetCharG = false;

    int totalModemReceiveDataBytes = 0;
    int totalErrorCount = 0;
    boolean bDataReceived = false;
    boolean bReceiveFirstPacket = false;
    boolean bDuplicatedPacket = false;

    boolean bUartModeTaskSet = true;
    boolean bReadDataProcess = true;
    // X modem -//

    // Y modem +//
    final int Y_MODEM_WAIT_ASK_SEND_FILE = 0;
    final int Y_MODEM_SEND_FILE_INFO_PACKET = 1;
    final int Y_MODEM_SEND_FILE_INFO_PACKET_WAIT_ACK = 2;
    final int Y_MODEM_START_SEND_FILE = 3;
    final int Y_MODEM_START_SEND_FILE_WAIT_ACK = 4;
    final int Y_MODEM_START_SEND_FILE_RESEND = 5;
    final int Y_MODEM_SEND_EOT_PACKET = 6;
    final int Y_MODEM_SEND_EOT_PACKET_WAIT_ACT = 7;
    final int Y_MODEM_SEND_LAST_END_PACKET = 8;
    final int Y_MODEM_SEND_LAST_END_PACKET_WAIT_ACK = 9;
    final int Y_MODEM_SEND_FILE_DONE = 10;

    final int DATA_NONE = 0;
    final int DATA_ACK = 1;
    final int DATA_CHAR_C = 2;
    final int DATA_NAK = 3;

    int ymodemState = 0;
    String modemFileName;
    String modemFileSize;
    int modemRemainData = 0;
    // Y modem -//

    // Z modem +//
    final int ZCRC_HEAD_SIZE = 4;

    final byte ZPAD = 0x2A; // '*' 052 Padding character begins frames
    final byte ZDLE = 0x18;
    final byte ZDLEE = ZDLE ^ 0100;   /* Escaped ZDLE as transmitted */

    final byte ZBIN = 0x41;        // 'A' Binary frame indicator (CRC-16)
    final byte ZHEX = 0x42;        // 'B' HEX frame indicator
    final byte ZBIN32 = 0x43;    // 'C' Binary frame with 32 bit CRC

    final byte LF = 0x0A;
    final byte CR = 0x0D;

    final int ZRQINIT = 0;   /* Request receive init */
    final int ZRINIT = 1;   /* Receive init */
    final int ZSINIT = 2;    /* Send init sequence (optional) */
    final int ZACK = 3;      /* ACK to above */
    final int ZFILE = 4;     /* File name from sender */
    final int ZSKIP = 5;     /* To sender: skip this file */
    final int ZNAK = 6;      /* Last packet was garbled */
    final int ZABORT = 7;    /* Abort batch transfers */
    final int ZFIN = 8;      /* Finish session */
    final int ZRPOS = 9;     /* Resume data trans at this position */
    final int ZDATA = 10;    /* Data packet(s) follow */
    final int ZDATA_HEADER = 21;
    final int ZFIN_ACK = 22;

    final int ZEOF = 11;     /* End of file */
    final int ZFERR = 12;    /* Fatal Read or Write error Detected */
    final int ZCRC = 13;     /* Request for file CRC and response */
    final int ZCHALLENGE = 14;   /* Receiver's Challenge */
    final int ZCOMPL = 15;   /* Request is complete */
    final int ZCAN = 16;     /* Other end canned session with CAN*5 */
    final int ZFREECNT = 17; /* Request for free bytes on filesystem */
    final int ZCOMMAND = 18; /* Command from sending program */
    final int ZSTDERR = 19;  /* Output to standard error, data follows */
    final int ZOO = 20;

    final int ZCRCE = 0x68; // no data
    final int ZCRCG = 0x69; // more data
    final int ZCRCW = 0x6B; // file info end

    final int ZDLE_END_SIZE_4 = 4; // zdle ZCRC? crc1 crc2
    final int ZDLE_END_SIZE_5 = 5; // zdle ZCRC? zdle crc1 crc2 || zdle ZCRC? crc1 zdle crc2
    final int ZDLE_END_SIZE_6 = 6; // zdle ZCRC? zdle crc1 zdle crc2

    final int ZF0 = 3;   /* First flags byte */
    final int ZF1 = 2;
    final int ZF2 = 1;
    final int ZF3 = 0;
    final int ZP0 = 0;   /* Low order 8 bits of position */
    final int ZP1 = 1;
    final int ZP2 = 2;
    final int ZP3 = 3;   /* High order 8 bits of file position */

    int zmodemState = 0;

    // fixed pattern, used to check ZRQINIT
    final int ZMS_0 = 0;
    final int ZMS_1 = 1; // r
    final int ZMS_2 = 2; // z
    final int ZMS_3 = 3; // \r
    final int ZMS_4 = 4; // ZPAD (ZRQINIT)
    final int ZMS_5 = 5; // ZPAD
    final int ZMS_6 = 6; // ZDLE
    final int ZMS_7 = 7; // ZHEX
    final int ZMS_8 = 8; // 0x30
    final int ZMS_9 = 9; // 0x30
    final int ZMS_10 = 10; // 0x30
    final int ZMS_11 = 11; // 0x30
    final int ZMS_12 = 12; // 0x30
    final int ZMS_13 = 13; // 0x30
    final int ZMS_14 = 14; // 0x30
    final int ZMS_15 = 15; // 0x30
    final int ZMS_16 = 16; // 0x30
    final int ZMS_17 = 17; // 0x30
    final int ZMS_18 = 18; // 0x30
    final int ZMS_19 = 19; // 0x30
    final int ZMS_20 = 20; // 0x30
    final int ZMS_21 = 21; // 0x30 (14th 0x30)
    final int ZMS_22 = 22; // 0x0D
    final int ZMS_23 = 23; // 0x0A
    final int ZMS_24 = 24; // 0x11
    int zmStartState = 0;
    // Z modem -//


    final String CLI_ON = "cli on\r\n";
    final String CD_MGR = "cd mgr\r\n";
    final String GETTX = "gettx\r\n";
    final String SETTX = "settx ";
    final String SETTP50W = "settp 50000\r\n";
    final String GETTP = "gettp\r\n";
    final String SETTP = "settp ";
    final String GETPAINFO = "getpainfo\r\n";
    final String POWER_ON = "setpainfo 0 3 1\r\n";
    final String POWER_OFF = "setpainfo 1 3 0\r\n";

    public final int NOP = 0;
    public final int GET_FREQ = 1;
    public final int GET_POWER = 2;
    public final int GET_POWER_STATE = 3;

    int getCommand = NOP;


    // general data count
    int totalReceiveDataBytes = 0;
    int totalUpdateDataBytes = 0;

    //    SelectFileDialog fileDialog;
    File mPath = new File(android.os.Environment.getExternalStorageDirectory() + "//DIR//");
    File fGetFile = null;

    static RelativeLayout mMenuSetting;
    static RelativeLayout mMenuKey;

    long back_button_click_time;
    boolean bBackButtonClick = false;


    // thread to read the data
    HandlerThread handlerThread; // update data to UI
    ReadThread readThread; // read data from USB
    ConnectionThread connectionThread;//Этот поток должен проверять, не отвалился ли com-порт

    // graphical objects
    TextView uartInfo;
    TextView contentFormatText;
    ScrollView scrollView;
    TextView readText;
    EditText writeText;
    Spinner baudSpinner;
    Spinner stopSpinner;
    Spinner dataSpinner;
    Spinner paritySpinner;
    Spinner flowSpinner;
    Spinner portSpinner;
    ArrayAdapter<CharSequence> baudAdapter;
    ArrayAdapter<CharSequence> portAdapter;

    Button btnConnect;
    Button btnWriteFreq, btnReadFreq;
    Button btnReadPower, btnWritePower, btnWritePower50W;
    Button btnPowerON, btnPowerOFF;
    Button btnGetPowerState;

    EditText etFreq, etPower;

    TextView tvPowerState;

    boolean bSendButtonClick = false;
    boolean bLogButtonClick = false;
    boolean bFormatHex = false;
    boolean bSendHexData = false;

    CharSequence contentCharSequence; // contain entire text content
    boolean bContentFormatHex = false;
    int contentFontSize = 12;
    boolean bWriteEcho = true;

    // show information message while send data by tapping "Write" button in hex content format
    int timesMessageHexFormatWriteData = 0;

    // note: when this values changed, need to check main.xml - android:id="@+id/ReadValues - android:maxLines="5000"
    final int TEXT_MAX_LINE = 1000;

    // variables
    final int UI_READ_BUFFER_SIZE = 10240; // Notes: 115K:1440B/100ms, 230k:2880B/100ms
    byte[] writeBuffer;
    byte[] readBuffer;
    char[] readBufferToChar;
    int actualNumBytes;

    int baudRate; /* baud rate */
    byte stopBit; /* 1:1stop bits, 2:2 stop bits */
    byte dataBit; /* 8:8bit, 7: 7bit */
    byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    byte flowControl; /* 0:none, 1: CTS/RTS, 2:DTR/DSR, 3:XOFF/XON */
    public Context global_context;
    boolean uart_configured = false;

    String uartSettings = "";

    //public static final int maxReadLength = 256;
    byte[] usbdata;
    char[] readDataToText;
    public int iavailable = 0;

    // file access//
    FileInputStream inputstream;
    FileOutputStream outputstream;

    FileWriter file_writer;
    FileReader file_reader;
    FileInputStream fis_open;
    FileOutputStream fos_save;
    BufferedOutputStream buf_save;
    boolean WriteFileThread_start = false;

    String fileNameInfo;
    String sFileName;
    int iFileSize = 0;
    int sendByteCount = 0;
    long start_time, end_time;
    long cal_time_1, cal_time_2;

    // data buffer
    byte[] writeDataBuffer;
    byte[] readDataBuffer; /* circular buffer */

    int iTotalBytes;
    int iReadIndex;

    final int MAX_NUM_BYTES = 65536;

    boolean bReadTheadEnable = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() запущен.");
        try {
            ftD2xx = D2xxManager.getInstance(this);
            Log.d(TAG, "D2xxManager.getInstance успешно.");
        } catch (D2xxManager.D2xxException e) {
            Log.d(TAG, "getInstance fail!!");
        }

        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            global_context = this;

            // init modem variables
            // file explore settings:
            // init UI objects
            Log.d(TAG, "Инициируем объекты графического интерфейса.");
            uartInfo = (TextView) findViewById(R.id.UartInfo);

            scrollView = (ScrollView) findViewById(R.id.ReadField);
            readText = (TextView) findViewById(R.id.ReadValues);

            btnConnect = (Button) findViewById(R.id.btnConnect);
            btnWriteFreq = (Button) findViewById(R.id.btnWriteFreq);
            btnReadFreq = (Button) findViewById(R.id.btnReadFreq);
            btnWritePower = (Button) findViewById(R.id.btnWritePower);
            btnReadPower = (Button) findViewById(R.id.btnReadPower);
            btnWritePower50W = (Button) findViewById(R.id.btnWritePower50W);
            btnPowerON = (Button) findViewById(R.id.btnPowerON);
            btnPowerOFF = (Button) findViewById(R.id.btnPowerOFF);
            btnGetPowerState = (Button) findViewById(R.id.btnGetPowerState);

            etFreq = (EditText) findViewById(R.id.etFreq);
            etPower = (EditText) findViewById(R.id.etPower);

            tvPowerState = (TextView) findViewById(R.id.tvPowerState);

            /* allocate buffer */
            Log.d(TAG, "allocate buffer");
            writeBuffer = new byte[512];
            readBuffer = new byte[UI_READ_BUFFER_SIZE];
            readBufferToChar = new char[UI_READ_BUFFER_SIZE];
            readDataBuffer = new byte[MAX_NUM_BYTES];
            actualNumBytes = 0;

            // start main text area read thread
            Log.d(TAG, "start main text area read thread");
            handlerThread = new HandlerThread(handler);
            handlerThread.start();

            /* setup the baud rate list*/
            baudRate = 115200;

            /* stop bits */
            stopBit = 1;

		    /* data bits */
            dataBit = 8;

		    /* parity */
            parity = 0;

		    /* flow control */
            flowControl = 0;

            /* port */
            Log.d(TAG, "Настрайваем спиннер portsList.");
            portSpinner = (Spinner) findViewById(R.id.portsList);// получаем экземпляр элемента Spinner
            // используем адаптер данных
            Log.d(TAG, "Используем адаптер данных.");
            portAdapter = ArrayAdapter.createFromResource(this, R.array.port_list_1,
                    android.R.layout.simple_list_item_1);
            // Вызываем адаптер
            Log.d(TAG, "Вызываем адаптер.");
            portSpinner.setAdapter(portAdapter);
//            portIndex = 0;
            Log.d(TAG, "Назначаем обработчик событий спиннеру portsList.");
            portSpinner.setOnItemSelectedListener(new MyOnPortSelectedListener());
            Log.d(TAG, "Назначили.");

            Log.d(TAG, "Назначаем обработчик кнопке btnConnect.");
            btnConnect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // always check whether there is a device or not
                    createDeviceList();
                    if (DevCount > 0) {
                        connectFunction();
                    }

                    if (DeviceStatus.DEV_NOT_CONNECT == checkDevice()) {
                        return;
                    }

                    setConfig(baudRate, dataBit, stopBit, parity, flowControl);

                    uart_configured = true;

                    // определяем массив типа String
//                    int tempDevCount = ftD2xx.createDeviceInfoList(global_context);
//                    D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[tempDevCount];
//                    if (ftD2xx.getDeviceInfoList(tempDevCount, deviceList) > 0) {
//                        int i = portIndex;
//                        readText.append("flags: " + deviceList[i].flags + "\n");
//                        readText.append("bcdDevice: " + deviceList[i].bcdDevice + "\n");
//                        readText.append("type: " + deviceList[i].type + "\n");
//                        readText.append("iSerialNumber: " + deviceList[i].iSerialNumber + "\n");
//                        readText.append("id: " + deviceList[i].id + "\n");
//                        readText.append("location: " + deviceList[i].location + "\n");
//                        readText.append("serialNumber: " + deviceList[i].serialNumber + "\n");
//                        readText.append("description: " + deviceList[i].description + "\n");
//                        readText.append("handle: " + deviceList[i].handle + "\n");
//                        readText.append("breakOnParam: " + deviceList[i].breakOnParam + "\n");
//                        readText.append("modemStatus: " + deviceList[i].modemStatus + "\n");
//                        readText.append("lineStatus: " + deviceList[i].lineStatus + "\n");
//                    }
                    // start connectionThread thread
                    Log.d(TAG, "start connectionThread thread");
                    connectionThread = new ConnectionThread(handler);
                    connectionThread.start();
                    String sendString = CLI_ON;
                    appendData("Отправлена команда: " + sendString);
                    int numBytes = sendString.length();
                    writeBuffer = new byte[numBytes];
                    for (int i = 0; i < numBytes; i++) {
                        writeBuffer[i] = (byte) (sendString.charAt(i));
                    }
                    sendData(numBytes, writeBuffer);

                    sendString = CD_MGR;
                    appendData("Отправлена команда: " + sendString);
                    numBytes = sendString.length();
                    writeBuffer = new byte[numBytes];
                    for (int i = 0; i < numBytes; i++) {
                        writeBuffer[i] = (byte) (sendString.charAt(i));
                    }
                    sendData(numBytes, writeBuffer);

                }
            });
            Log.d(TAG, "Назначили.");

            Log.d(TAG, "Назначаем общий обработчик для всех других кнопок.");
            btnWriteFreq.setOnClickListener(this);
            btnReadFreq.setOnClickListener(this);
            btnWritePower.setOnClickListener(this);
            btnWritePower50W.setOnClickListener(this);
            btnReadPower.setOnClickListener(this);
            btnPowerON.setOnClickListener(this);
            btnPowerOFF.setOnClickListener(this);
            btnGetPowerState.setOnClickListener(this);
            Log.d(TAG, "Назначили.");

//            connectToPort();

            Log.d(TAG, "onCreate() закончил работу.");
        } catch (Exception e) {
            Log.d(TAG, "При старте приложения произошла ошибка: " + e);
            Toast toast = Toast.makeText(getApplicationContext(),
                    "При старте приложения произошла ошибка: " + e, Toast.LENGTH_SHORT);
            toast.show();
            Log.d(TAG, "При старте приложения произошла ошибка: " + e);
            StackTraceElement[] stackTraceElements = e.getStackTrace();

            for (int i = 0; i < stackTraceElements.length; i++) {
                Log.d(TAG, i + ": " + stackTraceElements[i].toString());
            }
            this.onDestroy();
        }
    }

    // j2xx functions +
    public void createDeviceList() {
        Log.d(TAG, "createDeviceList() запущен.");
        int tempDevCount = ftD2xx.createDeviceInfoList(global_context); //Запрашиваем число подключенных COM-портов
        Log.d(TAG, "Текущее число COM-портов: " + DevCount);
        Log.d(TAG, "Число обнаруженных COM-портов: " + tempDevCount);
        if (tempDevCount > 0) { //Если COM-портов больше 0, то
            if (DevCount != tempDevCount) {//Если число COM-портов не равно текущему, то
                DevCount = tempDevCount;//обновляем число COM-портов
                updatePortNumberSelector();//Перерисовываем спиннер выбора COM-портов
            }
        } else {// иначе COM-портов нет
            DevCount = -1;
            currentPortIndex = -1;
        }
        Log.d(TAG, "createDeviceList() закончил работу.");
        Log.d(TAG, "Текущее число COM-портов: " + DevCount);
    }

    public void updatePortNumberSelector() {
        Log.d(TAG, "updatePortNumberSelector() запущен.");
        midToast("Число обнаруженных COM-портов: " + DevCount, Toast.LENGTH_SHORT);

        switch (DevCount) {
            case 2:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_2, android.R.layout.simple_list_item_1);
                break;
            case 3:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_3, android.R.layout.simple_list_item_1);
                break;
            case 4:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_4, android.R.layout.simple_list_item_1);
                break;
            case 5:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_5, android.R.layout.simple_list_item_1);
                break;
            case 6:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_6, android.R.layout.simple_list_item_1);
                break;
            case 7:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_7, android.R.layout.simple_list_item_1);
                break;
            case 8:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_8, android.R.layout.simple_list_item_1);
                break;
            case 1:
            default:
                portAdapter = ArrayAdapter.createFromResource(global_context, R.array.port_list_1, android.R.layout.simple_list_item_1);
                break;
        }

//        portAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        portSpinner.setAdapter(portAdapter);
        portAdapter.notifyDataSetChanged();
        Log.d(TAG, "updatePortNumberSelector() закончил работу.");
    }

    // call this API to show message
    void midToast(String str, int showTime) {
        Toast toast = Toast.makeText(global_context, str, showTime);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);

        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.YELLOW);
        toast.show();
    }

    public class MyOnPortSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Log.d(TAG, "onItemSelected в AdapterView.OnItemSelectedListener запущен.");
            portIndex = Integer.parseInt(parent.getItemAtPosition(pos).toString());
            Log.d(TAG, "portIndex = " + portIndex);
            Log.d(TAG, "onItemSelected в AdapterView.OnItemSelectedListener закончил работу.");
        }

        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(TAG, "onNothingSelected в AdapterView.OnItemSelectedListener запущен.");
            Log.d(TAG, "onNothingSelected в AdapterView.OnItemSelectedListener закончил работу.");
        }
    }

    public void connectFunction() {
        Log.d(TAG, "connectFunction() запущен.");

        if (portIndex + 1 > DevCount) {
            portIndex = 0;
        }

        if (currentPortIndex == portIndex
                && ftDev != null
                && true == ftDev.isOpen()) {
            midToast("Порт(" + portIndex + ") уже открыт.", Toast.LENGTH_SHORT);
            Log.d(TAG, "connectFunction() закончил работу.");
            return;
        } else {
            closePort();//Если был открыт другой порт, то закрываем его
        }

        if (true == bReadTheadEnable) {
            bReadTheadEnable = false;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (null == ftDev) {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        } else {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        }
        uart_configured = false;

        if (ftDev == null) {
            midToast("Неудачное открытие порта(" + portIndex + ")", Toast.LENGTH_LONG);
            Log.d(TAG, "connectFunction() закончил работу.");
            return;
        }

        if (true == ftDev.isOpen()) {
            currentPortIndex = portIndex;
            midToast("Порт(" + portIndex + ") открыт", Toast.LENGTH_SHORT);

            if (false == bReadTheadEnable) {
                readThread = new ReadThread(handler);
                readThread.start();
            }
        } else {
            midToast("Неудачное открытие порта(" + portIndex + ")", Toast.LENGTH_LONG);
        }
        Log.d(TAG, "connectFunction() закончил работу.");
    }

    DeviceStatus checkDevice() {
        Log.d(TAG, "checkDevice() запущен.");
        if (ftDev == null || false == ftDev.isOpen()) {
            midToast("Ничего не подключено.", Toast.LENGTH_SHORT);
            Log.d(TAG, "checkDevice() закончил работу.");
            return DeviceStatus.DEV_NOT_CONNECT;
        } else if (false == uart_configured) {
            //midToast("CHECK: uart_configured == false", Toast.LENGTH_SHORT);
            midToast("Нужно настроить UART.", Toast.LENGTH_SHORT);
            Log.d(TAG, "checkDevice() закончил работу.");
            return DeviceStatus.DEV_NOT_CONFIG;
        }
        Log.d(TAG, "checkDevice() закончил работу.");
        return DeviceStatus.DEV_CONFIG;

    }

    class ReadThread extends Thread {
        final int USB_DATA_BUFFER = 8192;
        Handler mHandler;

        ReadThread(Handler h) {
            mHandler = h;
            this.setPriority(MAX_PRIORITY);
        }

        public void run() {
            Log.d(TAG, "Поток чтения запущен.");
            byte[] usbdata = new byte[USB_DATA_BUFFER];
            int readcount = 0;
            int iWriteIndex = 0;
            bReadTheadEnable = true;

            while (true == bReadTheadEnable) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

//                Log.d(TAG, "iTotalBytes:" + iTotalBytes);
                while (iTotalBytes > (MAX_NUM_BYTES - (USB_DATA_BUFFER + 1))) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                readcount = ftDev.getQueueStatus();
                //Log.e(">>@@","iavailable:" + iavailable);
                if (readcount > 0) {
                    if (readcount > USB_DATA_BUFFER) {
                        readcount = USB_DATA_BUFFER;
                    }
                    ftDev.read(usbdata, readcount);

//                    if ((MODE_X_MODEM_CHECKSUM_SEND == transferMode)
//                            || (MODE_X_MODEM_CRC_SEND == transferMode)
//                            || (MODE_X_MODEM_1K_CRC_SEND == transferMode)) {
//                        for (int i = 0; i < readcount; i++) {
//                            modemDataBuffer[i] = usbdata[i];
//                            DLog.e(TXS, "RT usbdata[" + i + "]:(" + usbdata[i] + ")");
//                        }
//
//                        if (NAK == modemDataBuffer[0]) {
//                            DLog.e(TXS, "get response - NAK");
//                            bModemGetNak = true;
//                        } else if (ACK == modemDataBuffer[0]) {
//                            DLog.e(TXS, "get response - ACK");
//                            bModemGetAck = true;
//                        } else if (CHAR_C == modemDataBuffer[0]) {
//                            DLog.e(TXS, "get response - CHAR_C");
//                            bModemGetCharC = true;
//                        }
//                        if (CHAR_G == modemDataBuffer[0]) {
//                            DLog.e(TXS, "get response - CHAR_G");
//                            bModemGetCharG = true;
//                        }
//                    } else {
                    totalReceiveDataBytes += readcount;
                    //DLog.e(TT,"totalReceiveDataBytes:"+totalReceiveDataBytes);

                    //DLog.e(TT,"readcount:"+readcount);
                    for (int count = 0; count < readcount; count++) {
                        readDataBuffer[iWriteIndex] = usbdata[count];
                        iWriteIndex++;
                        iWriteIndex %= MAX_NUM_BYTES;
                    }

                    if (iWriteIndex >= iReadIndex) {
                        iTotalBytes = iWriteIndex - iReadIndex;
                    } else {
                        iTotalBytes = (MAX_NUM_BYTES - iReadIndex) + iWriteIndex;
                    }

                    //DLog.e(TT,"iTotalBytes:"+iTotalBytes);
//                        if ((MODE_X_MODEM_CHECKSUM_RECEIVE == transferMode)
//                                || (MODE_X_MODEM_CRC_RECEIVE == transferMode)
//                                || (MODE_X_MODEM_1K_CRC_RECEIVE == transferMode)
//                                || (MODE_Y_MODEM_1K_CRC_RECEIVE == transferMode)
//                                || (MODE_Z_MODEM_RECEIVE == transferMode)
//                                || (MODE_Z_MODEM_SEND == transferMode)) {
//                            modemReceiveDataBytes[0] += readcount;
//                            Log.d(TAG, "modemReceiveDataBytes:" + modemReceiveDataBytes[0]);
//                        }
//                    }
                }
            }

            Log.d(TAG, "Поток чтения прерван...");
            ;
        }
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "Обработчик событий запущен, пришло событие: " + msg);
            switch (msg.what) {
                case UPDATE_TEXT_VIEW_CONTENT:
                    if (actualNumBytes > 0) {
                        totalUpdateDataBytes += actualNumBytes;
                        for (int i = 0; i < actualNumBytes; i++) {
                            readBufferToChar[i] = (char) readBuffer[i];
                        }
                        appendData(String.copyValueOf(readBufferToChar, 0, actualNumBytes));
                    }
                    break;

//                case UPDATE_SEND_FILE_STATUS: {
//                    String temp = currentProtocol;
//                    if (sendByteCount <= 10240)
//                        temp += " Send:" + sendByteCount + "B("
//                                + new java.text.DecimalFormat("#.00").format(sendByteCount / (iFileSize / (double) 100)) + "%)";
//                    else
//                        temp += " Send:" + new java.text.DecimalFormat("#.00").format(sendByteCount / (double) 1024) + "KB("
//                                + new java.text.DecimalFormat("#.00").format(sendByteCount / (iFileSize / (double) 100)) + "%)";
//
//                    updateStatusData(temp);
//                }
//                break;

//                case UPDATE_SEND_FILE_DONE: {
//                    midToast("Send file Done.", Toast.LENGTH_SHORT);
//
//                    String temp = currentProtocol;
//                    if (0 == iFileSize) {
//                        temp += " - The sent file is 0 byte";
//                    } else if (iFileSize < 100) {
//                        temp += " Send:" + sendByteCount + "B("
//                                + new java.text.DecimalFormat("#.00").format(sendByteCount * 100 / iFileSize) + "%)";
//                    } else {
//                        if (sendByteCount <= 10240)
//                            temp += " Send:" + sendByteCount + "B("
//                                    + new java.text.DecimalFormat("#.00").format(sendByteCount / (iFileSize / (double) 100)) + "%)";
//                        else
//                            temp += " Send:" + new java.text.DecimalFormat("#.00").format(sendByteCount / (double) 1024) + "KB("
//                                    + new java.text.DecimalFormat("#.00").format(sendByteCount / (iFileSize / (double) 100)) + "%)";
//                    }
//
//                    Double diffime = (double) (end_time - start_time) / 1000;
//                    temp += " in " + diffime.toString() + " seconds";
//
//                    updateStatusData(temp);
//
//                    resetSendButton();
//                }
//                break;

//                case ACT_SELECT_SAVED_FILE_NAME:
//                    setProtocolMode();
//
//                    DLog.e(TT, "ACT_SELECT_SAVED_FILE_NAME transferMode:" + transferMode + " UART:" + (bUartModeTaskSet ? "True" : "False"));
//                    saveFileAction();
//                    break;
//
//                case ACT_SELECT_SAVED_FILE_FOLDER:
//                    getSelectedFolder();
//                    break;
//
//                case ACT_SAVED_FILE_NAME_CREATED:
//                    setProtocolMode();
//
//                    DLog.e(TT, "ACT_SAVED_FILE_NAME_CREATED transferMode:" + transferMode + " UART:" + (bUartModeTaskSet ? "True" : "False"));
//                    fGetFile = new File((String) msg.obj);
//                    saveFileAction();
//                    break;
//
//                case ACT_SELECT_SEND_FILE_NAME:
//                    setProtocolMode();
//
//                    sendFileAction();
//                    break;
//
//                case MSG_SELECT_FOLDER_NOT_FILE:
//                    midToast("Do not pick a file.\n" +
//                            "Plesae press \"Select Directory\" button to select current directory.", Toast.LENGTH_LONG);
//                    break;
//
//                case MSG_XMODEM_SEND_FILE_TIMEOUT: {
//                    String temp = currentProtocol + " - No response when send file.";
//                    midToast(temp, Toast.LENGTH_LONG);
//                    updateStatusData(temp);
//
//                    resetSendButton();
//                }
//                break;
//
//                case UPDATE_MODEM_RECEIVE_DATA:
//                    midToast(currentProtocol + " - Receiving data...", Toast.LENGTH_LONG);
//
//                case UPDATE_MODEM_RECEIVE_DATA_BYTES: {
//                    String temp = currentProtocol;
//                    if (totalModemReceiveDataBytes <= 10240)
//                        temp += " Receive " + totalModemReceiveDataBytes + "Bytes";
//                    else
//                        temp += " Receive " + new java.text.DecimalFormat("#.00").format(totalModemReceiveDataBytes / (double) 1024) + "KBytes";
//
//                    updateStatusData(temp);
//                }
//                break;
//
//                case UPDATE_MODEM_RECEIVE_DONE: {
//                    saveFileActionDone();
//
//                    String temp = currentProtocol;
//                    if (totalModemReceiveDataBytes <= 10240)
//                        temp += " Receive " + totalModemReceiveDataBytes + "Bytes";
//                    else
//                        temp += " Receive " + new java.text.DecimalFormat("#.00").format(totalModemReceiveDataBytes / (double) 1024) + "KBytes";
//
//                    Double diffime = (double) (end_time - start_time) / 1000;
//                    temp += " in " + diffime.toString() + " seconds";
//
//                    updateStatusData(temp);
//                }
//                break;

//                case MSG_MODEM_RECEIVE_PACKET_TIMEOUT: {
//                    midToast(currentProtocol + " - No Incoming Data.", Toast.LENGTH_LONG);
//                    String temp = currentProtocol;
//                    if (totalModemReceiveDataBytes <= 10240)
//                        temp += " Receive " + totalModemReceiveDataBytes + "Bytes";
//                    else
//                        temp += " Receive " + new java.text.DecimalFormat("#.00").format(totalModemReceiveDataBytes / (double) 1024) + "KBytes";
//
//                    updateStatusData(temp);
//                    saveFileActionDone();
//                }
//                break;
//
//                case ACT_MODEM_SELECT_SAVED_FILE_FOLDER:
//                    setProtocolMode();
//
//                    getModemSelectedFolder();
//                    break;
//
//                case MSG_MODEM_OPEN_SAVE_FILE_FAIL:
//                    midToast(currentProtocol + " - Open save file fail!", Toast.LENGTH_LONG);
//                    break;
//
//                case MSG_YMODEM_PARSE_FIRST_PACKET_FAIL:
//                    midToast("YModem - Can't parse packet due to incorrect data format!", Toast.LENGTH_LONG);
//                    resetLogButton();
//                    break;
//
//                case MSG_FORCE_STOP_SEND_FILE:
//                    midToast("Stop sending file.", Toast.LENGTH_LONG);
//                    break;

//                case UPDATE_ASCII_RECEIVE_DATA_BYTES: {
//                    String temp = currentProtocol;
//                    if (totalReceiveDataBytes <= 10240)
//                        temp += " Receive " + totalReceiveDataBytes + "Bytes";
//                    else
//                        temp += " Receive " + new java.text.DecimalFormat("#.00").format(totalReceiveDataBytes / (double) 1024) + "KBytes";
//
//                    long tempTime = System.currentTimeMillis();
//                    Double diffime = (double) (tempTime - start_time) / 1000;
//                    temp += " in " + diffime.toString() + " seconds";
//
//                    updateStatusData(temp);
//                }
//                break;
//
//                case UPDATE_ASCII_RECEIVE_DATA_DONE:
//                    saveFileActionDone();
//                    break;
//
//                case MSG_FORCE_STOP_SAVE_TO_FILE:
//                    midToast("Stop saving to file.", Toast.LENGTH_LONG);
//                    break;
//
//                case UPDATE_ZMODEM_STATE_INFO:
//                    updateStatusData("zmodemState:" + zmodemState);
//
//                    if (ZOO == zmodemState) {
//                        midToast("ZModem revice file done.", Toast.LENGTH_SHORT);
//                    }
//                    break;
//
//                case ACT_ZMODEM_AUTO_START_RECEIVE:
//                    bUartModeTaskSet = false;
//                    transferMode = MODE_Z_MODEM_RECEIVE;
//                    currentProtocol = "ZModem";
//
//
//                    receivedPacketNumber = 1;
//                    modemReceiveDataBytes[0] = 0;
//                    totalModemReceiveDataBytes = 0;
//                    bDataReceived = false;
//                    bReceiveFirstPacket = false;
//                    fileNameInfo = null;
//
//                    setLogButton();
//
//                    zmodemState = ZRINIT;
//                    start_time = System.currentTimeMillis();
//                    ZModemReadDataThread zmReadThread = new ZModemReadDataThread(handler);
//                    zmReadThread.start();
//                    break;


                case MSG_SPECIAL_INFO:

                    midToast("INFO:" + (String) (msg.obj), Toast.LENGTH_LONG);
                    break;

                case MSG_UNHANDLED_CASE:
                    if (msg.obj != null)
                        midToast("UNHANDLED CASE:" + (String) (msg.obj), Toast.LENGTH_LONG);
                    else
                        midToast("UNHANDLED CASE ?", Toast.LENGTH_LONG);
                    break;
                default:
                    midToast("NG CASE", Toast.LENGTH_LONG);
                    //Toast.makeText(global_context, ".", Toast.LENGTH_SHORT).show();
                    break;
            }
            Log.d(TAG, "Обработчик событий закончил работу.");
        }
    };

    void appendData(String data) {
        Log.d(TAG, "appendData(String data) запущен.");
        Log.d(TAG, "Обрабатываем команду: " + getCommand);
        switch (getCommand) {
            case NOP:
                if (true == bContentFormatHex) {
                    if (timesMessageHexFormatWriteData < 3) {
                        timesMessageHexFormatWriteData++;
                        midToast("The writing data won't be showed on data area while content format is hexadecimal format.", Toast.LENGTH_LONG);
                    }
                    Log.d(TAG, "appendData(String data) закончил работу.");
                    return;
                }

                if (true == bSendHexData) {
                    SpannableString text = new SpannableString(data);
                    text.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, data.length(), 0);
                    readText.append(text);
                    bSendHexData = false;
                } else {
                    readText.append(data);
                }

                int overLine = readText.getLineCount() - TEXT_MAX_LINE;

                if (overLine > 0) {
                    int IndexEndOfLine = 0;
                    CharSequence charSequence = readText.getText();

                    for (int i = 0; i < overLine; i++) {
                        do {
                            IndexEndOfLine++;
                        }
                        while (IndexEndOfLine < charSequence.length() && charSequence.charAt(IndexEndOfLine) != '\n');
                    }

                    if (IndexEndOfLine < charSequence.length()) {
                        readText.getEditableText().delete(0, IndexEndOfLine + 1);
                    } else {
                        readText.setText("");
                    }
                }

                scrollView.smoothScrollTo(0, readText.getHeight() + 30);
                break;
            case GET_FREQ:
                etFreq.setText(data);
                getCommand = NOP;
                readText.append(data);
                break;
            case GET_POWER:
                etPower.setText(data);
                getCommand = NOP;
                readText.append(data);
                break;
            case GET_POWER_STATE:
                tvPowerState.setText(data);
                getCommand = NOP;
                readText.append(data);
                break;
        }
        Log.d(TAG, "appendData(String data) закончил работу.");
    }

    // Update UI content
    class HandlerThread extends Thread {
        Handler mHandler;

        HandlerThread(Handler h) {
            mHandler = h;
        }

        public void run() {
            Log.d(TAG, "Поток Update UI content запущен.");
            byte status;
            Message msg;

            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (true == bContentFormatHex) // consume input data at hex content format
                {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
                } else if (MODE_GENERAL_UART == transferMode) {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);

                    if (0x00 == status) {
//                        if (false == WriteFileThread_start) {
//                            checkZMStartingZRQINIT();
//                        }

                        // save data to file
                        if (true == WriteFileThread_start && buf_save != null) {
                            try {
                                buf_save.write(readBuffer, 0, actualNumBytes);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        msg = mHandler.obtainMessage(UPDATE_TEXT_VIEW_CONTENT);
                        mHandler.sendMessage(msg);
                    }
                }

            }

        }
    }
    class ConnectionThread extends Thread {
        Handler mHandler;

        ConnectionThread(Handler h) {
            mHandler = h;
        }

        public void run() {
            Log.d(TAG, "Поток ConnectionThread запущен.");
            byte status;
            Message msg;

            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkDevice();
//                if (true == bContentFormatHex) // consume input data at hex content format
//                {
//                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
//                } else if (MODE_GENERAL_UART == transferMode) {
//                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
//
//                    if (0x00 == status) {
////                        if (false == WriteFileThread_start) {
////                            checkZMStartingZRQINIT();
////                        }
//
//                        // save data to file
//                        if (true == WriteFileThread_start && buf_save != null) {
//                            try {
//                                buf_save.write(readBuffer, 0, actualNumBytes);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        msg = mHandler.obtainMessage(UPDATE_TEXT_VIEW_CONTENT);
//                        mHandler.sendMessage(msg);
//                    }
//                }

            }

        }
    }

    // For zmWaitReadData: Write data at offset of buffer.
    byte readData(int numBytes, int offset, byte[] buffer) {
//        Log.d(TAG, "readData() запущен.");
        byte intstatus = 0x00; /* success by default */

		/* should be at least one byte to read */
        if ((numBytes < 1) || (0 == iTotalBytes)) {
            actualNumBytes = 0;
            intstatus = 0x01;
//            Log.d(TAG, "readData() закончил работу.");
            return intstatus;
        }

        if (numBytes > iTotalBytes) {
            numBytes = iTotalBytes;
        }

		/* update the number of bytes available */
        iTotalBytes -= numBytes;
        actualNumBytes = numBytes;

		/* copy to the user buffer */
        for (int count = offset; count < numBytes + offset; count++) {
            buffer[count] = readDataBuffer[iReadIndex];
            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }
//        Log.d(TAG, "readData() закончил работу.");
        return intstatus;
    }

    byte readData(int numBytes, byte[] buffer) {
//        Log.d(TAG, "readData() запущен.");
        byte intstatus = 0x00; /* success by default */

		/* should be at least one byte to read */
        if ((numBytes < 1) || (0 == iTotalBytes)) {
            actualNumBytes = 0;
            intstatus = 0x01;
//            Log.d(TAG, "readData() закончил работу.");
            return intstatus;
        }

        if (numBytes > iTotalBytes) {
            numBytes = iTotalBytes;
        }

		/* update the number of bytes available */
        iTotalBytes -= numBytes;
        actualNumBytes = numBytes;

		/* copy to the user buffer */
        for (int count = 0; count < numBytes; count++) {
            buffer[count] = readDataBuffer[iReadIndex];
            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }
//        Log.d(TAG, "readData() закончил работу.");
        return intstatus;
    }

    void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        // configure port
        // reset to UART mode for 232 devices
        Log.d(TAG, "setConfig() запущен.");
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        ftDev.setFlowControl(flowCtrlSetting, XON, XOFF);

        setUARTInfoString();
        midToast(uartSettings, Toast.LENGTH_SHORT);

        uart_configured = true;
        Log.d(TAG, "setConfig() закончил работу.");
    }

    void setUARTInfoString() {
        Log.d(TAG, "setUARTInfoString() запущен.");
        String parityString, flowString;

        switch (parity) {
            case 0:
                parityString = new String("None");
                break;
            case 1:
                parityString = new String("Odd");
                break;
            case 2:
                parityString = new String("Even");
                break;
            case 3:
                parityString = new String("Mark");
                break;
            case 4:
                parityString = new String("Space");
                break;
            default:
                parityString = new String("None");
                break;
        }

        switch (flowControl) {
            case 0:
                flowString = new String("None");
                break;
            case 1:
                flowString = new String("CTS/RTS");
                break;
            case 2:
                flowString = new String("DTR/DSR");
                break;
            case 3:
                flowString = new String("XOFF/XON");
                break;
            default:
                flowString = new String("None");
                break;
        }

        uartSettings = "Port " + portIndex + "; UART Setting  -  Baudrate:" + baudRate + "  StopBit:" + stopBit
                + "  DataBit:" + dataBit + "  Parity:" + parityString
                + "  FlowControl:" + flowString;

        resetStatusData();
        Log.d(TAG, "setUARTInfoString() закончил работу.");
    }

    void resetStatusData() {
        Log.d(TAG, "resetStatusData() запущен.");
        String tempStr = "Format - " + (bContentFormatHex ? "Hexadecimal" : "Character") + "\n" + uartSettings;
        String tmp = tempStr.replace("\\n", "\n");
        uartInfo.setText(tmp);
        Log.d(TAG, "resetStatusData() закончил работу.");
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Обработчик нажатия кнопок запущен.");
        if (DeviceStatus.DEV_CONFIG != checkDevice()) {
            return;
        }
        int numBytes;
        String sendString;
        switch (v.getId()) {
            case (R.id.btnReadFreq):
                sendString = "gettx\r\n";
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                getCommand = GET_FREQ;
                break;
            case (R.id.btnWriteFreq):
                sendString = SETTX + etFreq.getText().toString() + "\r\n";
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                break;
            case (R.id.btnReadPower):
                sendString = "gettp\r\n";
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                getCommand = GET_POWER;
                break;
            case (R.id.btnWritePower):
                sendString = SETTP + etPower.getText().toString() + "\r\n";
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                break;
            case (R.id.btnWritePower50W):
                sendString = SETTP50W;
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                break;
            case (R.id.btnPowerON):
                sendString = POWER_ON;
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                break;
            case (R.id.btnPowerOFF):
                sendString = POWER_OFF;
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                break;
            case (R.id.btnGetPowerState):
                sendString = "getpainfo\r\n";
                appendData("Отправлена команда: " + sendString);
                numBytes = sendString.length();
                writeBuffer = new byte[numBytes];
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) (sendString.charAt(i));
                }
                sendData(numBytes, writeBuffer);
                getCommand = GET_POWER_STATE;
                break;
        }
        Log.d(TAG, "Обработчик нажатия кнопок закончил работу.");
    }

    void sendData(int numBytes, byte[] buffer) {
        Log.d(TAG, "sendData() запущен.");
        if (ftDev.isOpen() == false) {
            Log.d(TAG, "SendData: device not open");
            Toast.makeText(global_context, "Device not open!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "sendData() закончил работу.");
            return;
        }

        if (numBytes > 0) {
            ftDev.write(buffer, numBytes);
        }
        Log.d(TAG, "sendData() закончил работу.");
    }

    void connectToPort() {
        Log.d(TAG, "connectToPort() запущен.");
        // always check whether there is a device or not
        createDeviceList();
//             определяем массив типа String
//        int tempDevCount = ftD2xx.createDeviceInfoList(global_context);
//        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[tempDevCount];
//        int i = portIndex;
//        if (ftD2xx.getDeviceInfoList (tempDevCount, deviceList) > 0) {
//            readText.append("flags: " + deviceList[i].flags + "\n");
//            readText.append("bcdDevice: " + deviceList[i].bcdDevice + "\n");
//            readText.append("type: " + deviceList[i].type + "\n");
//            readText.append("iSerialNumber: " + deviceList[i].iSerialNumber + "\n");
//            readText.append("id: " + deviceList[i].id + "\n");
//            readText.append("location: " + deviceList[i].location + "\n");
//            readText.append("serialNumber: " + deviceList[i].serialNumber + "\n");
//            readText.append("description: " + deviceList[i].description + "\n");
//            readText.append("handle: " + deviceList[i].handle + "\n");
//            readText.append("breakOnParam: " + deviceList[i].breakOnParam + "\n");
//            readText.append("modemStatus: " + deviceList[i].modemStatus + "\n");
//            readText.append("lineStatus: " + deviceList[i].lineStatus + "\n");
//        }

        if (DevCount > 0) {
            connectFunction();
        }

        if (DeviceStatus.DEV_NOT_CONNECT == checkDevice()) {
            Log.d(TAG, "connectToPort() закончил работу.");
            return;
        }

        setConfig(baudRate, dataBit, stopBit, parity, flowControl);

        uart_configured = true;
        Log.d(TAG, "connectToPort() закончил работу.");
    }

    public void disconnectFunction() {
        Log.d(TAG, "disconnectFunction() запущен.");
        DevCount = -1;
        currentPortIndex = -1;
        bReadTheadEnable = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ftDev != null) {
            if (true == ftDev.isOpen()) {
                ftDev.close();
            }
        }
        Log.d(TAG, "disconnectFunction() закончил работу.");
    }

    public void closePort() {
        Log.d(TAG, "closePort() запущен.");
        bReadTheadEnable = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ftDev != null) {
            if (true == ftDev.isOpen()) {
                ftDev.close();
            }
        }
        Log.d(TAG, "closePort() закончил работу.");
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart() запущен.");
        super.onStart();
//        createDeviceList();
//        if (DevCount > 0) {
//            connectFunction();
//            setUARTInfoString();
//            setConfig(baudRate, dataBit, stopBit, parity, flowControl);
//        }
        Log.d(TAG, "onStart() закончил работу.");
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart() запущен.");
        super.onRestart();
        Log.d(TAG, "onRestart() закончил работу.");
    }

    protected void onResume() {
        Log.d(TAG, "onResume() запущен.");
        super.onResume();
        if (null == ftDev || false == ftDev.isOpen()) {
            Log.d(TAG, "Первый запуск или все порты закрыты, заново создаем список подключенных устройств");
            createDeviceList();
            if (DevCount == 1) {
                Log.d(TAG, "Обнаружено всего одно устройство, сразу подключаемся к нему.");
                portIndex = 0;
                btnConnect.performClick();
            }
        }
        Log.d(TAG, "onResume() закончил работу.");
    }

    protected void onPause() {
        Log.d(TAG, "onPause() запущен.");
        super.onPause();
        Log.d(TAG, "onPause() закончил работу.");
    }

    protected void onStop() {
        Log.d(TAG, "onStop() запущен.");
        super.onStop();
        Log.d(TAG, "onStop() закончил работу.");
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy() запущен.");
        disconnectFunction();
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
        Log.d(TAG, "onDestroy() закончил работу.");
    }
}
