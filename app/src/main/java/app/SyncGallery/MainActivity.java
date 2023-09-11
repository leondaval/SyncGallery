package app.SyncGallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import android.graphics.BitmapFactory;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

public class MainActivity extends AppCompatActivity {

    private int NotificationId = 1; // ID per la notifica
    private Uri directoryUri;  // Il valore deve essere mantenuto alla chiusura dell'app in modo che ad ogni riavvio ricorda il path scelto dall'utente per la copia e lo spostamento dei file.
    //private String directoryUriString;  Variabile di appoggio per convertire URI (PATH) in stringa.
    //SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE); Inizializzazione delle preferenze condivise dell'app

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //directoryUriString = prefs.getString("directoryUri", null);
        //    if (directoryUriString != null)  Se != null allora app gia avviata e premuto tasto copia, sposta o cambia directory.
        //        directoryUri = Uri.parse(directoryUriString);  Se è presente una uri (directory) nelle preferenze, allora la inserisce nella variabile uri.

        PermessoNotifiche();

        Button copyDirectoryButton = findViewById(R.id.copyDirectoryButton);
        copyDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    if (directoryUri != null) {
                        if (Copy())
                            Toast.makeText(MainActivity.this, "Copia eseguita con successo!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Errore, copia non riuscita!", Toast.LENGTH_SHORT).show();
                    } else
                        openDirectory();
                } else
                    requestPermission();

            }
        });

        Button moveDirectoryButton = findViewById(R.id.moveDirectoryButton);
        moveDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    if (directoryUri != null) {
                        if (Move())
                            Toast.makeText(MainActivity.this, "Spostamento eseguito con successo!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Errore, spostamento non riuscito!", Toast.LENGTH_SHORT).show();
                    } else
                        openDirectory();
                } else
                    requestPermission();

            }
        });


        Button changeDirectoryButton = findViewById(R.id.changeDirectoryButton);
        changeDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission())
                    openDirectory();
                 else
                    requestPermission();
            }
        });


        Button syncDirectoryButton = findViewById(R.id.syncDirectoryButton);
        syncDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission() && checkPermissionInternet())
                    showSmbCredentialsDialog();
                 else {
                    if (!checkPermission())
                        requestPermission();
                    else
                        requestPermissionInternet();
                }

            }
        });
    }

    private boolean checkPermissionInternet() {
        int networkStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        return networkStatePermission == PackageManager.PERMISSION_GRANTED &&
                internetPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void PermessoNotifiche() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (!notificationManager.areNotificationsEnabled()) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }

    }

    private void requestPermissionInternet() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted)
                Toast.makeText(MainActivity.this, "Puoi utilizzare la rete!", Toast.LENGTH_SHORT).show();
             else
                Toast.makeText(MainActivity.this, "Permesso di utilizzo rete negato!", Toast.LENGTH_SHORT).show();

        }
    }


    private boolean checkPermission() {
        return Environment.isExternalStorageManager();
    }

    private void requestPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        requestPermissionLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            });

    private ActivityResultLauncher<Intent> directoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        directoryUri = data.getData();
                        String srcdirtemp = directoryUri.getPath();
                        String srcdir;
                        int colonIndex = srcdirtemp.indexOf(':');
                        srcdir = srcdirtemp.substring(colonIndex + 1);
                        Toast.makeText(MainActivity.this, "Directory cambiata con successo!", Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivity.this, srcdir, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void openDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryLauncher.launch(intent);
    }

    private boolean Copy() {
        boolean success = true;
        String srcdirtemp = directoryUri.getPath();
        String srcdir;
        int colonIndex = srcdirtemp.indexOf(':');

        srcdir = srcdirtemp.substring(colonIndex + 1);

        Toast.makeText(MainActivity.this, srcdir, Toast.LENGTH_SHORT).show();

        srcdir = "/sdcard/" + srcdir;

        success &= copyDirectory(new File(srcdir), new File("/sdcard/DCIM/SYNC"));
        return success;
    }

    private boolean copyDirectory(File srcDir, File dstDir) {
        if (!dstDir.exists())
            dstDir.mkdirs();

        

        // Mostra la notifica all'inizio del processo di copia
        showProgressNotification("Copia in corso...", 0, true);

        try {
            int totalFiles = 0;
            for (File srcFile : srcDir.listFiles()) {
                if (srcFile.isFile() && (srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".jpeg") || srcFile.getName().endsWith(".mp4") || srcFile.getName().endsWith(".webp") || srcFile.getName().endsWith(".png")))
                    totalFiles++;
            }

            int copiedFiles = 0;
            for (File srcFile : srcDir.listFiles()) {
                if (srcFile.isFile() && (srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".jpeg") || srcFile.getName().endsWith(".mp4") || srcFile.getName().endsWith(".webp") || srcFile.getName().endsWith(".png"))) {
                    File dstFile = new File(dstDir, srcFile.getName());
                    InputStream in = new FileInputStream(srcFile);
                    OutputStream out = new FileOutputStream(dstFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                    copiedFiles++;

                    // Calcola lo stato del processo e aggiorna la notifica con lo stato
                    int progress = (copiedFiles * 100) / totalFiles;
                    showProgressNotification("Copia in corso...", progress, true);
                }
            }

            // Mostra la notifica di completamento
            showProgressNotification("Copia eseguita con successo!", -1, false);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showProgressNotification("Copia fallita!", -1, false);
            return false;
        }
    }

    private boolean Move() {
        boolean success = true;
        String srcdirtemp = directoryUri.getPath();
        String srcdir;
        int colonIndex = srcdirtemp.indexOf(':');

        srcdir = srcdirtemp.substring(colonIndex + 1);

        Toast.makeText(MainActivity.this, srcdir, Toast.LENGTH_SHORT).show();

        srcdir = "/sdcard/" + srcdir;

        success &= moveDirectory(new File(srcdir), new File("/sdcard/DCIM/SYNC"));
        return success;
    }

    private boolean moveDirectory(File srcDir, File dstDir) {
        if (!dstDir.exists())
            dstDir.mkdirs();

        // Mostra la notifica all'inizio del processo di spostamento
        showProgressNotification("Spostamento in corso...", 0, true);

        try {
            int totalFiles = 0;
            for (File srcFile : srcDir.listFiles()) {
                if (srcFile.isFile() && (srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".jpeg") || srcFile.getName().endsWith(".mp4") || srcFile.getName().endsWith(".webp") || srcFile.getName().endsWith(".png"))) {
                    totalFiles++;
                }
            }

            int movedFiles = 0;
            for (File srcFile : srcDir.listFiles()) {
                if (srcFile.isFile() && (srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".jpeg") || srcFile.getName().endsWith(".mp4") || srcFile.getName().endsWith(".webp") || srcFile.getName().endsWith(".png"))) {
                    File dstFile = new File(dstDir, srcFile.getName());
                    if (!srcFile.renameTo(dstFile))
                        return false;

                    movedFiles++;

                    // Calcola lo stato del processo e aggiorna la notifica con lo stato
                    int progress = (movedFiles * 100) / totalFiles;
                    showProgressNotification("Spostamento in corso...", progress, true);
                }
            }

            // Mostra la notifica di completamento
            showProgressNotification("Spostamento eseguito con successo!", -1, false);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showProgressNotification("Spostamento fallito!", -1, false);
            return false;
        }
    }


    private void showProgressNotification(String message, int progress, boolean isOngoing) {
        // Controlla se l'app possiede i permessi necessari
        if (checkPermission()) {
            // Crea un canale di notifica Android
            NotificationChannel canale = new NotificationChannel("canale", "Notifiche", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canale);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "canale")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle("Processo avviato!")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(isOngoing);

            if (progress >= 0 && progress <= 100)
                builder.setProgress(100, progress, false);

            NotificationManagerCompat.from(MainActivity.this).notify(NotificationId, builder.build());
        } else
            Toast.makeText(MainActivity.this, "Errore, permesso non concesso!", Toast.LENGTH_SHORT).show();
    }


    private void showSmbCredentialsDialog() {
        String dstDirPath = "/sdcard/DCIM/SYNC";
        boolean successo = true;
        // Verifica se la cartella "SYNC" esiste e, se necessario, la crea
        File syncDir = new File(dstDirPath);
        if (!syncDir.exists()) {
            boolean created = syncDir.mkdirs();
            if (!created) {
                Toast.makeText(MainActivity.this, "Impossibile creare la cartella SYNC!", Toast.LENGTH_LONG).show();
                successo = false;
            } else
                Toast.makeText(MainActivity.this, "Cartella SYNC creata con successo!", Toast.LENGTH_LONG).show();

        }
        if (successo) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_smb_credentials, null);
            EditText usernameEditText = view.findViewById(R.id.usernameEditText);
            EditText passwordEditText = view.findViewById(R.id.passwordEditText);
            EditText smbUrlEditText = view.findViewById(R.id.smbUrlEditText);
            builder.setView(view)
                    .setPositiveButton("Avvia sync", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String username = usernameEditText.getText().toString();
                            String password = passwordEditText.getText().toString();
                            String smbUrl = smbUrlEditText.getText().toString();
                            moveDirectoryToSMB(new java.io.File("/sdcard/DCIM/SYNC"), smbUrl, "BACKUP", username, password);
                        }
                    })
                    .setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(MainActivity.this, "Sincronizzazione cancellata dall'utente!", Toast.LENGTH_LONG).show();
                        }
                    });
            builder.create().show();
        }
    }

    private void moveDirectoryToSMB(File localDir, String smbUrl, String shareName, String username, String password) {
        // Controllo dei permessi
        if (!checkPermission()) {
            Toast.makeText(MainActivity.this, "Errore, permesso non concesso", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifica se la cartella SYNC è vuota
        if (localDir.listFiles() == null || localDir.listFiles().length == 0) {
            showProgressNotification("Directory SYNC vuota!", -1, false);
            return;
        }

        // Mostra la notifica all'inizio del processo di copia
        showProgressNotification("Spostamento in corso...", 0, true);

        final boolean[] successo = {false}; // Variabile per tenere traccia se la sincronizzazione è andata a buon fine

        // All'interno del metodo onClick o dove viene chiamato il processo di spostamento
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SMBClient client = new SMBClient();
                    try (Connection connection = client.connect(smbUrl)) {
                        AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                        Session session = connection.authenticate(ac);
                        // Connect to the specified share name
                        try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                            int totalFiles = 0;
                            int movedFiles = 0;

                            for (File localFile : localDir.listFiles()) {
                                if (localFile.isFile()) {
                                    totalFiles++;
                                }
                            }

                            for (File localFile : localDir.listFiles()) {
                                if (localFile.isFile()) {
                                    try {
                                        FileInputStream in = new FileInputStream(localFile);
                                        com.hierynomus.smbj.share.File smbFile = share.openFile(localFile.getName(),
                                                EnumSet.of(AccessMask.GENERIC_ALL),
                                                null,
                                                SMB2ShareAccess.ALL,
                                                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                                                null);
                                        OutputStream out = smbFile.getOutputStream();

                                        byte[] buffer = new byte[1024];
                                        int len;
                                        while ((len = in.read(buffer)) > 0) {
                                            out.write(buffer, 0, len);
                                        }

                                        // Chiudi gli stream
                                        in.close();
                                        out.close();

                                        // Effettua lo spostamento (taglia) del file locale
                                        if (localFile.delete()) {
                                            movedFiles++;

                                            // Calcola lo stato del processo e aggiorna la notifica con lo stato
                                            int progress = (movedFiles * 100) / totalFiles;
                                            showProgressNotification("Spostamento in corso...", progress, true);

                                            if (!successo[0] && movedFiles == totalFiles) {
                                                // Mostra la notifica di spostamento completato
                                                showProgressNotification("Spostamento completato", -1, false);

                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, "File spostati con successo!", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                successo[0] = true;
                                            }
                                        } else {
                                            showErrorMessage("Errore durante lo spostamento del file locale");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        showErrorMessage("Errore di I/O durante il trasferimento del file");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorMessage("Errore di connessione al server SMB");
                }
            }

            private void showErrorMessage(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Mostra la notifica di errore durante lo spostamento
                        showProgressNotification("Errore durante lo spostamento", -1, false);
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}