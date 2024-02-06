import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;


class CommClient {
    Socket clientS = null;
    BufferedReader in = null;
    PrintWriter out = null;
  
    CommClient() {}
    CommClient(String host,int port) { open(host,port); }
  
    // クライアントソケット(通信路)のオープン　
    // 接続先のホスト名とポート番号が必要．
    boolean open(String host,int port){
      try{
        clientS = new Socket(InetAddress.getByName(host), port);
        in = new BufferedReader(new InputStreamReader(clientS.getInputStream()));
        out = new PrintWriter(clientS.getOutputStream(), true);
      } catch (UnknownHostException e) {
        System.err.println("ホストに接続できません。");
        System.exit(1);
      } catch (IOException e) {
        System.err.println("IOコネクションを得られません。");
        System.exit(1);
      }
      return true;
    }
  
     // データ送信
     boolean send(String msg){
       if (out == null) { return false; }
       out.println(msg);
       return true;
     }
  
     // データ受信
     String recv(){
         String msg=null;
         if (in == null) { return null; }
         try{
           msg=in.readLine();
         } catch (SocketTimeoutException e){
             return null;
         } catch (IOException e) {
           System.err.println("受信に失敗しました。");
           System.exit(1);
         }
         return msg;
     }
  
     // タイムアウトの設定
     int setTimeout(int to){
         try{
           clientS.setSoTimeout(to);
         } catch (SocketException e){
           System.err.println("タイムアウト時間を変更できません．");
           System.exit(1);
         }
         return to;
     }
  
     // ソケットのクローズ (通信終了)
     void close(){
       try{
         in.close();  out.close();
         clientS.close();
       } catch (IOException e) {
           System.err.println("ソケットのクローズに失敗しました。");
           System.exit(1);
       }
       in=null; out=null;
       clientS=null;
     }
 }

 

//Model
class ClientModel implements ActionListener {
    private CommClient cl;
    private TextLabelFrame view;
    private DesignLabelFrame view2;
    private Controller controller;
    private String[] questions; //問題文
    private char c; //入力された文字
    private ArrayList<Character> ans; //現在までの回答
    private int n = 0; //n問目
    private int enemyn = 0;
    private int totalkey = 0, rightkey = 0, endtime = -1;
    private String myname;
    private String enename;
    private javax.swing.Timer timer;
    private int timecounter = 0;

    public ClientModel() {
      questions = new String[10];
      //ans = new ArrayList<Character>();
      cl = new CommClient("localhost", 10030);
      cl.setTimeout(20);
      do{questions[0] = cl.recv();}while(questions[0] == null);
    }

    public void gameStart() {
      String msg;
      cl.send("ok!");
      do{enename = cl.recv();}while(enename == null);
      do{msg = cl.recv();}while(msg == null);
      questions = msg.split(" ");
      n = 1; enemyn = 1;
      totalkey = 0; rightkey = 0; 
      endtime = -1; timecounter = 0;
      ans = new ArrayList<Character>();
      timer = new javax.swing.Timer(10, this);
      timer.start();
      this.view.updateAO();
    }

    public void setView(TextLabelFrame v) { //オブザーバー削除による変更
        this.view = v;
    }

    public void setView2(DesignLabelFrame v2) { //オブザーバー削除による変更
      this.view2 = v2;
    }

    public void setController(Controller c){
      this.controller = c;
    }

    public String getValuepro(){
        return this.questions[this.n];
    }

    public int getValuenum(){
      return this.n;
    }

    public String getValueans(){
        String currentanswer = "";
        int i;
        for(i = 0; i < ans.size(); i++){
          currentanswer = currentanswer + String.valueOf(ans.get(i));
        }
        return currentanswer;
    }

    public void setname(String name){
      this.myname = new String(name);
      cl.send(myname);
    }

    public void changecurrentchar(char c){
        this.c = c; this.totalkey++;
        char[] question = questions[n].toCharArray();
        if(this.c == question[this.ans.size()]){
          this.ans.add(c); this.rightkey++;
          this.view.updateAO();
          if(this.ans.size() == questions[n].length()){
            this.cl.send("END"+this.n);  
            if(n == 10) { System.out.println(this.getValueans()); cl.send("endProcess"); endProcess(); } //終了処理
            else if(this.enemyn == 11){ endProcess(); System.out.println("endProcess end"); }
            else { n++;}
            this.ans = new ArrayList<Character>();
          }
          this.view.updateAO(); //オブザーバー削除による変更
        }
    }


    public void endProcess(){
      String msg;
      this.endtime = this.timecounter;
      do{msg = this.cl.recv();}while(msg == null);
      System.out.println(msg + " endProcess");
      this.questions[this.n] = new String(msg);
      System.out.println(msg);
    }

    public void replayProcess(){
      String msg;
      //view.settextpro("Choice: Replay");
      this.questions[this.n] = new String("Choice: Replay");
      this.view.updateAO();
      cl.send("Replay");
      do{msg = this.cl.recv();}while(msg == null);
      if(msg.equals("Replay")){
        controller.setmode(0);
        view.settextans("Please type Space");
        do{questions[0] = cl.recv();}while(questions[0] == null);
        this.view.settextpro(new String(questions[0]));
      }else{
        view.settextans(new String(msg));
        controller.setmode(3);
      }
    }

    public void quitProcess(){
      String msg;
      controller.setmode(3);
      //view.settextpro("Choice: Quit");
      this.questions[this.n] = new String("Choice: Quit");
      this.view.updateAO();
      cl.send("Quit");
      do{msg = this.cl.recv();}while(msg == null);
      view.settextans(new String(msg));
    }

    public String printnum(){
      if(this.n == 11){
        return this.myname+" is finished";
      }else{
        return this.myname+" is in No."+String.valueOf(this.n);
      }
    }

    public String printenemynum(){
      if(this.enemyn == 11){
        return this.enename+" is finished";
      }else{
        return this.enename+" is in No."+String.valueOf(this.enemyn);
      }
    } 

    public void actionPerformed(ActionEvent e){
      String msg;
      timecounter += 10;
      if(endtime < 0){
        do{
          msg = cl.recv(); 
          if(msg != null){ this.enemyn = Integer.parseInt(msg); System.out.println("recv "+msg); } //ひとまず相手側が何問目か数値のみ送られてくる前提
        }while(msg != null);
        this.view2.updateA1();
      }else{
        if(timecounter == endtime+2000){
          int car = (int)((float)rightkey*100/(float)totalkey);
          this.questions[this.n] = new String("correct rate: "+car+"%"); this.view.updateAO();
          System.out.println("endtime: "+endtime);
          System.out.println("timecounter1: "+timecounter);
        }else if(timecounter == endtime+4000){
          int typespeed = rightkey*1000/endtime;
          this.questions[this.n] = new String("speed: "+typespeed+"key/sec"); this.view.updateAO();
          System.out.println("timecounter2: "+timecounter);
        }else if(timecounter == endtime+6000){
          this.questions[this.n] = new String("replay: \"r\" quit: \"q\""); this.view.updateAO();
          controller.setmode(2);
          System.out.println("timecounter2: "+timecounter);
        }

      }
    }
}

//Controller
class Sound {
  public Clip createClip(File path){
    //指定されたURLのオーディオ入力ストリームを取得
		try (AudioInputStream ais = AudioSystem.getAudioInputStream(path)){	
			//ファイルの形式取得
			AudioFormat af = ais.getFormat();
			//単一のオーディオ形式を含む指定した情報からデータラインの情報オブジェクトを構築
			DataLine.Info dataLine = new DataLine.Info(Clip.class,af);
			//指定された Line.Info オブジェクトの記述に一致するラインを取得
			Clip c = (Clip)AudioSystem.getLine(dataLine);
			//再生準備完了
			c.open(ais);
			return c;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		return null;
  }
}


class Controller implements KeyListener {
    private Sound s = new Sound();
    private ClientModel cmo;
    private int mode = 0;
    public Controller(ClientModel cm){
        this.cmo = cm;
    }

    public void setmode(int m){
      this.mode = m;
    }

    public void keyPressed(KeyEvent e){
      Clip clip = s.createClip(new File("./click1.wav"));
      clip.start();
      char c = e.getKeyChar();
      if(mode == 0) {
        if(c == ' ') { 
          System.out.println(c); //確認用
          mode = 1;
          cmo.gameStart(); 
        }
      }
      else if(mode == 1){
        System.out.println(c); //確認用
        cmo.changecurrentchar(c);   //clientmodelにあるchangecurrentcharで文字を読み取る
      }
      else if(mode == 2){
        if(c == 'r'){cmo.replayProcess();}
        else if(c == 'q'){cmo.quitProcess();}
      }
  }
    public void keyReleased(KeyEvent e){};
    public void keyTyped(KeyEvent e){};
}

//View

class TextLabelFrame extends JPanel { //JPanelに変更してMainFrameに貼り付ける
    private JLabel prolabel, anslabel; //prolabelは問題文, anslabelは解答
    private ClientModel cm;   //clientmodelにあるgetValuepro, getValueansを呼び出すため
    
    public TextLabelFrame(ClientModel cmodel, Controller ccont){
        this.setSize(500, 250);
        this.setBackground(Color.BLACK);
        this.setLayout(new GridLayout(2, 1));     //縦2横1
        cm = cmodel;
        prolabel = new JLabel(cm.getValuepro(), JLabel.CENTER);    //prolabelに問題文を貼り付ける
        prolabel.setFont(new Font("Century", Font.ITALIC, 50));
        prolabel.setForeground(Color.yellow);
        LineBorder border = new LineBorder(Color.yellow, 5, true);
        prolabel.setBorder(border);
        this.add(prolabel);
        anslabel = new JLabel("<html><span style='font-size:70pt; color:yellow; " 
        +"background-color:black;'>Please type Space</span></html>", JLabel.CENTER);    //anslabelに解答を貼り付ける
        anslabel.setFont(new Font("Century", Font.ITALIC, 50));
        anslabel.setForeground(Color.yellow);
        LineBorder border1 = new LineBorder(Color.WHITE, 2, true);
        anslabel.setBorder(border1);
        this.add(anslabel);
        this.setFocusable(true); // KeyEventを受け取るのはViewと思われる
        this.addKeyListener(ccont);
    }
    public void updateAO() {
      String pro;   //問題文を受け取るpro
      String ans;   //解答を受け取るans
      pro = cm.getValuepro();  //getValueproは問題文のstringを返す関数
      ans = cm.getValueans();  //getValueansはプレイヤーがどこまで打ったというstring
      prolabel.setText(pro);   //prolabelを更新
      prolabel.paintImmediately(prolabel.getVisibleRect());
      prolabel.setFont(new Font("Century", Font.ITALIC, 50));
      prolabel.setForeground(Color.yellow);
      System.out.println(ans+" before setText");
      anslabel.setText(ans);   //anslabelを更新
      anslabel.paintImmediately(anslabel.getVisibleRect());
      System.out.println(ans+" after setText");
      anslabel.setFont(new Font("Century", Font.ITALIC, 50));
      anslabel.setForeground(Color.yellow);
    }
    public void settextpro(String text){
      prolabel.setText(text);
    }
    public void settextans(String text){
      anslabel.setText(text);
    }
}

class DesignLabelFrame extends JPanel {
    private JLabel numlabel, enemylabel; //numlabelは自分が何問目か, enemylabelは相手が何問目か
    private ClientModel cm;   //clientmodelにあるgetValuepro, getValueansを呼び出すため
    public DesignLabelFrame(ClientModel cmodel, Controller ccont) {
      this.setSize(500, 250);
      this.setBackground(Color.BLACK);
      this.setLayout(new GridLayout(2, 1));     //縦2横1
      cm = cmodel;
      numlabel = new JLabel(cm.printnum(), JLabel.CENTER);    //numlabelに問題が何番目かを貼り付ける
      numlabel.setFont(new Font("Century", Font.ITALIC, 50));
      numlabel.setForeground(Color.yellow);
      this.add(numlabel);
      enemylabel = new JLabel(cm.printenemynum() , JLabel.CENTER);  //enemylabelに相手の問題が何番目かを貼り付ける
      enemylabel.setFont(new Font("Century", Font.ITALIC, 50));
      enemylabel.setForeground(Color.red);
      this.add(enemylabel);
      //this.setFocusable(true); // KeyEventを受け取るのはViewと思われる
      //this.addKeyListener(ccont);
  }

  public void updateA1() {
    String num;      //playerのn問目のn=num
    String enemynum; //相手のn問目のn=num
    num = cm.printnum();  //getValueplayernumはplayerの問題文の何問目か返す関数
    enemynum = cm.printenemynum();    //getValueenemynumは相手の問題文の何問目か返す関数
    numlabel.setText(num);   //numlabelを更新
    enemylabel.setText(enemynum);  //enemylabelを更新
} 

}

class GameFrame extends JPanel {
    private Controller controller;
    private TextLabelFrame view;
    private DesignLabelFrame view2;
    public GameFrame(ClientModel model){
        //this.model = model;
        controller = new Controller(model);
        view = new TextLabelFrame(model, controller);
        view2 = new DesignLabelFrame(model, controller);
        this.setLayout(new GridLayout(1, 2));
        model.setView2(this.view2); //オブザーバー削除による変更
        model.setView(this.view);
        model.setController(this.controller);
        this.add(view2, BorderLayout.CENTER);
        this.add(view, BorderLayout.CENTER);
        this.setSize(1000, 500);
    }
}

class StartFrame extends JPanel implements ActionListener{
    private MainFrameAO Main;
    private Sound s = new Sound();
    private ClientModel CM;
    JTextField Textfield;
    public StartFrame(ClientModel model, MainFrameAO main) {
        CM = model;
        Main = main;
        this.setSize(1000, 500);
        this.setBackground(Color.BLACK);
        this.setLayout(new GridLayout(4, 1));     //縦4横1
        JLabel label1 = new JLabel("<html><span style='font-size:60pt; color:#00FF7F; " 
        +"background-color:black;'>Communicative typing game</span></html>", JLabel.CENTER);  
        label1.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 60));
        LineBorder border = new LineBorder(Color.GREEN, 5, true);
        label1.setBorder(border);
        label1.setBackground(Color.BLACK);
        label1.setOpaque(true);
        this.add(label1);
        JLabel label = new JLabel("<html><span style='font-size:50pt; color:#00FF7F; " 
        +"background-color:black;'>Please write your name below.</span></html>", JLabel.CENTER);
        label.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 50));
        label.setBackground(Color.BLACK);
        label.setOpaque(true);
        this.add(label);
        Textfield = new JTextField();
        Textfield.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 40));
        this.add(Textfield);
        JButton btn = new JButton("Register");
        btn.setForeground(Color.GREEN);
        btn.setBackground(Color.BLACK);
        btn.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 24));
        this.add(btn);
        btn.addActionListener(this);
        //this.setTitle("TypingGame");
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);  
    }

    public void actionPerformed(ActionEvent e){
        Clip clip = s.createClip(new File("./click1.wav"));
        clip.start();
        String str = Textfield.getText();
        CM.setname(str);
        Main.gamevisible();
        this.setVisible(false);
    }
}


class MainFrameAO extends JFrame {
    private StartFrame start;
    private GameFrame game;
    private ClientModel model;
    public MainFrameAO(){
        model = new ClientModel();
        start = new StartFrame(model, this);
        this.add(start);
        start.setVisible(true);
        //game = new GameFrame(model);
        //this.add(game);
        //game.setVisible(false);
        this.setTitle("TypingGame");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1000, 500);
        this.setVisible(true);
    }

    public void gamevisible(){
      game = new GameFrame(model);
      this.add(game);
      game.setVisible(true);
    }
    public static void main(String argv[]){
        new MainFrameAO();
    }
}
