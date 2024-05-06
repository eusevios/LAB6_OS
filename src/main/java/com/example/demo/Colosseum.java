package com.example.demo;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static com.sun.jna.platform.win32.WinBase.INFINITE;

public class Colosseum extends Application implements Initializable {

    private static final Logger logger = LogManager.getLogger(Colosseum.class);

    public Text SPQR;
    @FXML
    public ProgressBar thracianBar;
    @FXML
    public ProgressBar samniteBar;
    @FXML
    public AnchorPane pane;
    public Button button;
    public TextField NTextField;
    public Text enterText;
    public ImageView sm_image;
    public ImageView th_image;
    public Text countdown;
    public Text scoreText;
    public Text th_damage;
    public Text sm_damage;
    public Text sm_name;
    public Text th_name;

    ServerSocket serverSocket;
    Socket[] currentSockets;

    WinNT.HANDLE battleEvent;

    WinNT.HANDLE[] gladiatorEvents;

    int[] score = { 0, 0 };

    int samnite_health = 50;
    int thracian_health = 50;
    int damage_samnite;
    int damage_thracian;

    @Override
    public void start(Stage primaryStage) throws IOException {

        logger.info("\n\nSERVER STARTED");

        primaryStage.setResizable(false);

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Colosseum.fxml")));

        Scene scene = new Scene(root, 850, 525);

        primaryStage.setScene(scene);
        primaryStage.setTitle("COLLOSEUM");

        primaryStage.show();

    }

    private void battle() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                int num = Integer.parseInt(NTextField.getText());
                score[0] = 0;
                score[1] = 0;
                setVisibleBattle(true);
                updateScore();

                for (int i = 0; i < num; i++) {
                    Kernel32.INSTANCE.WaitForMultipleObjects(2, gladiatorEvents, true, INFINITE);

                    samnite_health = 50;
                    thracian_health = 50;
                    byte[] buffer = new byte[1024];

                    String sm_number;
                    String th_number;

                    int bytesRead = currentSockets[0].getInputStream().read(buffer);
                    sm_number = new String(buffer, 0, bytesRead-2);
                    logger.info("the client sm-" + sm_number + " sent number");

                    bytesRead = currentSockets[1].getInputStream().read(buffer);
                    th_number = new String(buffer, 0, bytesRead-2);
                    logger.info("the client th-" + th_number + " sent number");

                    sm_name.setText("SAMNITE " + RomanConverter.toRoman(Integer.parseInt(sm_number)));
                    th_name.setText("THRAEX "  + RomanConverter.toRoman(Integer.parseInt(th_number)));


                    Platform.runLater(()-> {
                        samniteBar.setProgress(1.0);
                        thracianBar.setProgress(1.0);
                    });

                    setVisibleDamage(false);
                    countdown.setText("3");
                    Thread.sleep(1000);
                    countdown.setText("2");
                    Thread.sleep(1000);
                    countdown.setText("1");
                    Thread.sleep(1000);
                    countdown.setText("FIGHT!");
                    setVisibleDamage(true);

                    Kernel32.INSTANCE.SetEvent(battleEvent);

                    while(samnite_health > 0 && thracian_health > 0){

                        byte[] buf = currentSockets[0].getInputStream().readNBytes(1);

                        String str_damage_samnite = new String(buf, 0, 1);
                        logger.info("the client sm-" + sm_number + " damage: " + str_damage_samnite);

                        damage_samnite = Integer.parseInt(str_damage_samnite);

                        buf  = currentSockets[1].getInputStream().readNBytes(1);

                        String str_damage_thracian = new String(buf, 0, 1);
                        logger.info("the client th-" + th_number + " damage: " + str_damage_thracian);

                        damage_thracian = Integer.parseInt(str_damage_thracian);

                        samnite_health-=damage_thracian;
                        thracian_health-=damage_samnite;

                        Platform.runLater(()-> {
                            samniteBar.setProgress(Math.max(0,(double) samnite_health / 50));
                            thracianBar.setProgress(Math.max(0, (double) thracian_health / 50));
                        });

                        sm_damage.setText("-" + str_damage_thracian);
                        th_damage.setText("-" + str_damage_samnite);

                        Thread.sleep(350);

                    }
                    String s_msg, t_msg, winmsg;

                    if (thracian_health > 0) {
                        s_msg = "YOU DIED, SAMNITE!";
                        t_msg = "YOU ARE WINNER, THRACIAN!";
                        winmsg = "THRAEX WINS!";
                        score[1]+=1;
                    }

                    else if (samnite_health > 0) {
                        t_msg = "YOU DIED, THRACIAN!";
                        s_msg = "YOU ARE WINNER, SAMNITE!";
                        winmsg = "SAMNITE WINS!";
                        score[0]+=1;
                    }
                    else {
                        t_msg = "YOU DIED, THRACIAN!";
                        s_msg = "YOU DIED, SAMNITE!";
                        winmsg = "DRAW!";
                        score[0]+=1;
                        score[1]+=1;
                    }

                    countdown.setText(winmsg);
                    updateScore();
                    setVisibleDamage(false);
                    Thread.sleep(1000);


                    Kernel32.INSTANCE.ResetEvent(battleEvent);

                    PrintWriter out = new PrintWriter(currentSockets[0].getOutputStream(), true);
                    out.println(s_msg);
                    logger.info("message sent to client sm-" + sm_number);
                    out = new PrintWriter(currentSockets[1].getOutputStream(), true);
                    out.println(t_msg);
                    logger.info("message sent to client th-" + th_number);
                    logger.info("Client sm-" + sm_number + " disconnected");
                    logger.info("Client th-" + th_number + " disconnected");
                }
                if(score[0]>score[1]) countdown.setText("SAMNITES WON!");
                else if(score[0]<score[1]) countdown.setText("THRACIANS WON!");
                else countdown.setText("DRAW!");
                Thread.sleep(2000);

                setVisibleBattle(false);
                setVisibleMainMenu(true);


                return null;

            }

        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }



    public void updateScore(){
        scoreText.setText(score[0] + " : " + score[1]);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        samniteBar.setStyle("-fx-accent: #c27a13;");
        thracianBar.setStyle("-fx-accent: #1052c7;");
        setVisibleBattle(false);

        try {
            serverSocket = new ServerSocket(1111);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentSockets = new Socket[2];
        battleEvent = Kernel32.INSTANCE.CreateEvent(null, true, false, "BattleEvent");
        gladiatorEvents = new WinNT.HANDLE[2];
        gladiatorEvents[0] = Kernel32.INSTANCE.CreateEvent(null, false, false, "SAMNITE_EVENT");
        gladiatorEvents[1] = Kernel32.INSTANCE.CreateEvent(null, false, false, "THRACIAN_EVENT");



        enterText.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        SPQR.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        countdown.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        scoreText.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        sm_name.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        th_name.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        th_damage.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");
        sm_damage.setStyle("-fx-effect: dropshadow(three-pass-box, black, 5, 0.5, 0, 0);");

    }

    public static void main(String[] args) {
        launch(args);
    }

    public void buttonEvent() {

        String number = NTextField.getText();
        int num = Integer.parseInt(number);
        try {
            for(int i = 0; i < 2*num; i++){
                String command = "LAB5_CLIENT.exe";
                ProcessBuilder pb = new ProcessBuilder(command);

                pb.start();

                Socket clientSocket = serverSocket.accept();
                logger.info("Client " + i + " connected");

                int type = i % 2;
                int gl_num = i / 2 + 1;
                OutputStream outputStream = clientSocket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);
                out.println(type);
                out.println(gl_num);
                Thread thread = getThread(clientSocket);
                thread.start();

                setVisibleMainMenu(false);


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        battle();

    }

    private Thread getThread(Socket clientSocket) throws IOException {
        return new Thread(() -> {
            try {
                byte[] t = clientSocket.getInputStream().readNBytes(1);
                int type = Integer.parseInt( new String(t, 0, 1));

                currentSockets[type] = clientSocket;
                Kernel32.INSTANCE.SetEvent(gladiatorEvents[type]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setVisibleMainMenu(boolean bool){

        button.setVisible(bool);
        enterText.setVisible(bool);
        NTextField.setVisible(bool);

    }

    private void setVisibleBattle(boolean bool){

        thracianBar.setVisible(bool);
        samniteBar.setVisible(bool);
        scoreText.setVisible(bool);
        countdown.setVisible(bool);
        th_image.setVisible(bool);
        sm_image.setVisible(bool);
        th_name.setVisible(bool);
        sm_name.setVisible(bool);

    }
    private void setVisibleDamage(boolean bool){
        sm_damage.setVisible(bool);
        th_damage.setVisible(bool);

    }



}