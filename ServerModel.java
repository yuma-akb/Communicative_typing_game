import java.net.*;
import java.io.*;

class CommServer {
    private ServerSocket serverS = null;
    private Socket clientS = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private int port=0;
 
    CommServer() {}
    CommServer(int port) { open(port); }
    CommServer(CommServer cs) { serverS=cs.getServerSocket(); open(cs.getPortNo()); }
    
    ServerSocket getServerSocket() { return serverS; } 
    int getPortNo() { return port; }
 
    // サーバ用のソケット(通信路)のオープン
    // サーバ用のソケットはクライアントからの接続待ち専用．
    // ポート番号のみを指定する．
    boolean open(int port){
      this.port=port;
      try{ 
     if (serverS == null) { serverS = new ServerSocket(port); }
      } catch (IOException e) {
         System.err.println("ポートにアクセスできません。");
         System.exit(1);
      }
      try{
         clientS = serverS.accept();
         out = new PrintWriter(clientS.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(clientS.getInputStream()));
      } catch (IOException e) {
         System.err.println("Acceptに失敗しました。");
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
          //マルチクライアントにするとこのタイムアウトがひたすら起きるので、
          //エラーの出力はしないようにしておく。プログラム上関係ないので完成したら外す。
          //System.err.println("タイムアウトです．");
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
        clientS.close();  serverS.close();
      } catch (IOException e) {
          System.err.println("ソケットのクローズに失敗しました。");
          System.exit(1);
      }
      in=null; out=null;
      clientS=null; serverS=null;
    }
}

//問題文作るクラス。
class MakeWordList{
    public static String sendWords;
    public static String words[];
    public static int wordsNum = 2670;//TypingWordList.txtの単語数に合わせる。
    public static int qNum = 10; //問題数。 

    MakeWordList(){
        sendWords = chooseWords(words);
    }

    //問題文リストを配列に読み込む。
    //増やしたかったらMakeWordListのwordsNumを変更。
    public static void setWords(String[] words){
        try{
            File file = new File("TypingWordList.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str = null;
            int i = 0;
            while((str = br.readLine()) != null){
                words[i] = str; //ファイルの1行ずつ読み込んだ結果が入っている。
                i = i + 1;
            }
            br.close();
        }catch(FileNotFoundException e){
            System.out.println(e);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    //単語をwordsから10個選んでsendWordsという１つのStringに入れる。
    //単語同士はスペースを開ける。
    private static String chooseWords(String[] words){
        String choosenWord;
        String sendWords = "";
        int i = 0;
        int[] duplication = new int[wordsNum];
        while(i < qNum){
            int num = (int)(Math.random()*(words.length));
            if(duplication[num] == 1){continue;} //1度出た単語が選ばれた場合は選び直し。
            duplication[num] = 1;
            choosenWord = words[num];
            sendWords = sendWords + " " + choosenWord;
            i++;
        }
        return sendWords;
    }
}

//ゲームを動かすクラス。
class GameManager{
    static String msg;
    static int from, end; //単語集の単語数, プレイヤーの識別、終わった問題の番号。
    private int process1 = 1, process2 = 1, endCheck = 0;//10問終えたプレイヤー番号を加算。
    public GameManager(){};
    public GameManager(ServerModel sm, ServerModel sm2){

        sm.send("You are Player 1");
        sm2.send("You are Player 2");
        
        if(ServerModel.cont == false){
            do{sm.name = sm.recv();}while(sm.name == null);
            do{sm2.name = sm2.recv();}while(sm2.name == null);
        }
        
        while(sm.recv() == null){}; //Client双方がSpaceキーを押すのを待つ。
        while(sm2.recv() == null){};

        new MakeWordList(); //sendWordsに問題文を生成。

        //対戦相手の名前を送る。
        
        sm.send(sm2.name);
        sm2.send(sm.name);
        

        sm.send(MakeWordList.sendWords);
        sm2.send(MakeWordList.sendWords);

        //process1がプレイヤー1が現在解いている問題の番号。
        while(true){ //どちらかが10問解くまでこのループは続く。
            do{ //recv待ちループ。recvしたら何らかの処理をしてからbreak;。
                if((msg=sm.recv())!=null){
                    from = 1;
                    if(msg.equals("END"+MakeWordList.qNum)){//最後の問題が終わったかどうか。
                        endCheck = from;
                        process1 = endReceiver(msg) + 1;
                        sm2.send(""+process1); System.out.println("send 11");
                        break;
                    }
                    process1 = endReceiver(msg) + 1;
                    sm2.send(""+process1);
                    break;
                }
                if((msg=sm2.recv())!=null){
                    from = 2;
                    if(msg.equals("END"+MakeWordList.qNum)){
                        endCheck = from;
                        process2 = endReceiver(msg) + 1;
                        sm.send(""+process2);
                        break;
                    }
                    process2 = endReceiver(msg) + 1;
                    sm.send(""+process2);
                    break;
                }
            }while(true);
            if(endCheck != 0){
                break;
            }
        }
        do{msg = sm.recv();}while(msg == null);
        do{msg = sm2.recv();}while(msg == null);

        if(endCheck == 1){ //勝者の名前を送信。
            sm.send("Winner: "+ sm.name);
            sm2.send("Winner: "+ sm.name);
        }else if(endCheck == 2){
            sm.send("Winner: "+ sm2.name);
            sm2.send("Winner: "+ sm2.name);
        }

        //リプレイするかどうかをClient1,2それぞれで確認。
        do{sm.message = sm.recv();}while(sm.message == null); //"Replay" or "Quit"
        System.out.println("sm: "+sm.message); //debug
        do{sm2.message = sm2.recv();}while(sm2.message == null);
        System.out.println("sm2: "+sm2.message); //debug
        replayOrQuit(sm, sm2);
        sm.send(sm.message); 
        sm2.send(sm2.message);
    }

    //"END〇"というStringから〇をintで取り出す。
    private static int endReceiver(String endPhrase){
        endPhrase = endPhrase.replaceAll("[^0-9]","");
        int yourProcess = Integer.parseInt(endPhrase);
        return yourProcess;
    }

    //Client２つの意思をまとめる。相手だけQuitを選んだ場合ButQuitが入れられる。
    private static void replayOrQuit(ServerModel sm, ServerModel sm2){
        if(sm.message.equals("Replay") && sm2.message.equals("Replay")){
            sm.message = "Replay";
            sm2.message = "Replay";
            ServerModel.cont = true;
        }else if(sm.message.equals("Replay") && sm2.message.equals("Quit")){
            sm.message = "ButQuit";
            sm2.message = "Quit";
            ServerModel.cont = false;
        }else if(sm.message.equals("Quit") && sm2.message.equals("Replay")){
            sm.message = "Quit";
            sm2.message = "ButQuit";
            ServerModel.cont = false;
        }else{
            sm.message = "Quit";
            sm2.message = "ButQuit";
            ServerModel.cont = false;
        }
    }
}

public class ServerModel extends CommServer {
    String message, name; //各クライアントから送られるメッセージ、プレイヤー名。
    public static boolean cont;
    ServerModel(int port){
        super(port);
    }
    ServerModel(CommServer cs){
        super(cs);
    }

    
    //ServerとClientの接続をする。
    //ゲームの流れを操作するGameManagerを任意回数繰り返し呼び出す。
    public static void main(String[] args){
        ServerModel sm = new ServerModel(10030);
        ServerModel sm2 = new ServerModel(sm);
        sm.setTimeout(10);
        sm2.setTimeout(10);
        cont = false;
        //問題リストを配列に読み込むのは最初の1度だけやるようにする。
        MakeWordList.words = new String[MakeWordList.wordsNum];
        MakeWordList.setWords(MakeWordList.words);

        do{
            new GameManager(sm, sm2);
        }while(cont);
        sm.close();
        sm2.close();
    }    
}