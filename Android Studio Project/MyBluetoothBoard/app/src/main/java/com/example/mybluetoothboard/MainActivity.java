package com.example.mybluetoothboard;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    BluetoothDevice boardHc05;
    BluetoothSocket boardSocket = null;
    OutputStream boardSocketOutputStream = null;
    InputStream boardSocketInputStream = null;
    String validUuid = "00001101-0000-1000-8000-00805f9b34fb";

    Spinner alignmentSpinner, effectInSpinner, effectOutSpinner, btSpinner;
    SeekBar speedSeekBar, intensitySeekBar;
    EditText messageEditText, pauseEditText;
    Button updateButton, pauseSetButton;

    Boolean waitingForBoardConfirmation = false;


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageEditText = findViewById(R.id.messageEditText);
        pauseEditText = findViewById(R.id.pauseEditText);

        // Alignment Spinner
        alignmentSpinner = findViewById(R.id.alignmentSpinner);
        ArrayAdapter<String> alignmentSpinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.alignments));
        alignmentSpinner.setAdapter(alignmentSpinnerAdapter);

        // EffectIn Spinner
        effectInSpinner = findViewById(R.id.effectInSpinner);
        ArrayAdapter<String> effectInAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.effects));
        effectInSpinner.setAdapter(effectInAdapter);

        // EffectOut Spinner
        effectOutSpinner = findViewById(R.id.effectOutSpinner);
        ArrayAdapter<String> effectOutAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.effects));
        effectOutSpinner.setAdapter(effectOutAdapter);

        // Speed SeekBar
        speedSeekBar = findViewById(R.id.speedSeekBar);
        TextView speedValueTextView = findViewById(R.id.speedValueTextView);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                speedValueTextView.setText("" + i);
                sendSetSpeed(speedSeekBar.getMax() - speedSeekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Intensity SeekBar
        intensitySeekBar = findViewById(R.id.intensitySeekBar);
        TextView intensityValueTextView = findViewById(R.id.intensityValueTextView);
        intensitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                intensityValueTextView.setText("" + i);
                sendSetIntensity(intensitySeekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Pause set button onClickListener
        pauseSetButton = findViewById(R.id.pauseSetButton);
        pauseSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String value = pauseEditText.getText().toString();
                if (value.equals("")) return;
                sendSetPause(Integer.parseInt(value));
            }
        });

        ActivityResultLauncher<String> requestBluetoothPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                    } else {
                        Toast.makeText(MainActivity.this, "Bluetooth permission is necessary to communicate with the board.", Toast.LENGTH_SHORT).show();
                    }
                });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissionLauncher.launch(
                    Manifest.permission.BLUETOOTH);
            return;
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!btAdapter.isEnabled()) {
            Toast.makeText(MainActivity.this, "Enable Bluetooth and restart app.", Toast.LENGTH_SHORT).show();
            finish();
        }

        ArrayList<BluetoothDevice> bondedDevices = new ArrayList<BluetoothDevice>(btAdapter.getBondedDevices());
        ArrayList<String> bondedDevicesNames = new ArrayList<String>();
        for (BluetoothDevice device : bondedDevices) {
            bondedDevicesNames.add(device.getName());
        }

        // Bluetooth devices spinner
        btSpinner = findViewById(R.id.btSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, bondedDevicesNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        btSpinner.setAdapter(adapter);

        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedIndex = btSpinner.getSelectedItemPosition();

                if (selectedIndex == -1)
                    Toast.makeText(MainActivity.this, "Please select something.", Toast.LENGTH_SHORT).show();
                else {
                    boardHc05 = bondedDevices.get(selectedIndex);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
                        return;
                    }

                    // Check if device has HC05 Uuid
                    ParcelUuid[] Uuids = boardHc05.getUuids();
                    Boolean isValidChipUuid = false;
                    for (ParcelUuid Uuid : Uuids) {
                        if (Uuid.toString().equals(validUuid)) {
                            isValidChipUuid = true;
                            break;
                        }
                    }
                    if (!isValidChipUuid) {
                        Toast.makeText(MainActivity.this, "Please choose a valid board device.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TextView statusValueTextView = findViewById(R.id.statusValueTextView);
                    statusValueTextView.setText("Connecting...");
                    statusValueTextView.setTextColor(Color.rgb(255, 165, 0));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
                                return;
                            }

                            int counter = 0;
                            do {
                                try {
                                    boardSocket = boardHc05.createRfcommSocketToServiceRecord(UUID.fromString(validUuid));
                                    boardSocket.connect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                counter++;
                            } while (!boardSocket.isConnected() && counter < 1);

                            String deviceName = boardHc05.getName();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (boardSocket.isConnected()) {
                                        try {
                                            boardSocketOutputStream = boardSocket.getOutputStream();
                                            boardSocketInputStream = boardSocket.getInputStream();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        Toast.makeText(MainActivity.this, "Connection established with " + deviceName, Toast.LENGTH_LONG).show();
                                        statusValueTextView.setText("Connected");
                                        statusValueTextView.setTextColor(Color.GREEN);
                                        sendSetAllCommand("Device connected", 0, 2, 2, 25, 0, 10);
                                        refreshValuesAsync();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to connect, make sure the board is up and running.", Toast.LENGTH_LONG).show();
                                        statusValueTextView.setText("Disconnected");
                                        statusValueTextView.setTextColor(Color.RED);
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }
        });

        // Set updateButton's click listener
        updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pauseEditText.getText().toString().equals("")) return;
                sendSetAllCommand(messageEditText.getText().toString(),
                                  alignmentSpinner.getSelectedItemPosition(),
                                  effectInSpinner.getSelectedItemPosition(),
                                  effectOutSpinner.getSelectedItemPosition(),
                                  speedSeekBar.getMax() - speedSeekBar.getProgress(),
                                  Integer.parseInt(pauseEditText.getText().toString()),
                                  intensitySeekBar.getProgress());
            }
        });
    }

    private String readResponseFromBoard(long TimeoutMS) {
        // Waits for a string in the following format: `Response`, and only returns "Response"
        if (!boardSocket.isConnected() || boardSocketInputStream == null) return null;

        long startTime = System.currentTimeMillis();
        String data = "";
        try {
            while ( !(data.startsWith("`") && data.endsWith("`") && data.length() > 2) ) {
                if (boardSocketInputStream.available() > 0) {
                    char character = (char) boardSocketInputStream.read();
                    Log.d("Dtomper", String.valueOf(character));
                    if (character == '`' || data.startsWith("`"))  data += character;
                }
                if (System.currentTimeMillis() - startTime >= TimeoutMS) {
                    Log.d("Dtomper", "Timeout reached for bluetooth response");
                    boardSocketInputStream.skip(boardSocketInputStream.available());
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data.substring(1, data.length() - 1);
    }

    private void waitForBoardConfirmationAsync() {
        if (!boardSocket.isConnected() || boardSocketInputStream == null) return;
        try {
            boardSocketInputStream.skip(boardSocketInputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
        }

        waitingForBoardConfirmation = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                waitingForBoardConfirmation = !Objects.equals(readResponseFromBoard(10000), "OK");
            }
        }).start();
    }

    private void refreshValuesAsync() {
        if (boardSocket == null || !boardSocket.isConnected() || boardSocketInputStream == null || boardSocketOutputStream == null)
        {
            Toast.makeText(MainActivity.this, "Couldn't get values from board.", Toast.LENGTH_SHORT).show();
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Make sure no confirmation is being awaited from board
                while (waitingForBoardConfirmation) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Send command to board
                String command = "getAll";
                try {
                    boardSocketOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to send command to board.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                    return;
                }

                // Get response from board
                String response = readResponseFromBoard(10000);
                if (response == null) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Got empty response from board", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                Log.d("Response", response);
                String[] parameters = response.split("\\|"); // delimiter is |
                Log.d("Split", Arrays.asList(parameters).toString());

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageEditText.setText(parameters[0].trim());
                        alignmentSpinner.setSelection(Integer.parseInt(parameters[1]));
                        effectInSpinner.setSelection(Integer.parseInt(parameters[2]));
                        effectOutSpinner.setSelection(Integer.parseInt(parameters[3]));
                        speedSeekBar.setProgress(Integer.parseInt(parameters[4]));
                        intensitySeekBar.setProgress(Integer.parseInt(parameters[6]));
                        pauseEditText.setText(parameters[5]);
                    }
                });
            }
        }).start();
    }

    private void sendSetAllCommand(String text, int alignIndex, int effectInIndex, int effectOutIndex, int effectDelayMs, int effectPauseMs, int intensity) {
        if (boardSocket == null || !boardSocket.isConnected() || boardSocketOutputStream == null) return;
        String command = "setAll "
                         + text + "|"
                         + String.valueOf(alignIndex) + "|"
                         + String.valueOf(effectInIndex) + "|"
                         + String.valueOf(effectOutIndex) + "|"
                         + String.valueOf(effectDelayMs) + "|"
                         + String.valueOf(effectPauseMs) + "|"
                         + String.valueOf(intensity);
        try {
            boardSocketOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
            waitForBoardConfirmationAsync();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to send command to board.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendSetSpeed(int speed) {
        if (boardSocket == null || !boardSocket.isConnected() || boardSocketOutputStream == null) return;
        String command = "setDelay " + String.valueOf(speed);
        try {
            boardSocketOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to send speed to board.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendSetPause(int pause) {
        if (boardSocket == null || !boardSocket.isConnected() || boardSocketOutputStream == null) return;
        String command = "setPause " + String.valueOf(pause);
        try {
            boardSocketOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to send pause to board.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendSetIntensity(int intensity) {
        if (boardSocket == null || !boardSocket.isConnected() || boardSocketOutputStream == null) return;
        String command = "setIntensity " + String.valueOf(intensity);
        try {
            boardSocketOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to send intensity to board.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

}