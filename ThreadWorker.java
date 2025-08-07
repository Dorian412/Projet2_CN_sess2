import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

//doit suprimer le cookie pour continuer de tester car mon client a tjr le meme cookie 
//update jsplus pq j ai ecrit ca au dessus
//probleme il faut lancer deux fois la page pour qu il nous reconaise
//si on fait juste une fois le GET play.html et qu on joue ca bug et la session nous reconais pas 
//mais si on fait deux fois GET apres y a pas de pb 
//ca vient peut etre du moment ou je store le cookie dans mo session store

public class ThreadWorker implements Runnable{
    private Socket socket;
    private OutputStream outputStream;
    private InputStream requestStream;
    private PrintWriter responseStream;
    private Session session = null;
    private static final Map<String, Session> sessionStore = new ConcurrentHashMap<>();// faire un systeme pr que les perime se barrent
    private static final List<Session> finishedGame = Collections.synchronizedList(new ArrayList<>());
    private static boolean firstLaunch = true;//variable pour reset le cookie sinon erreur lors du redemarage du serveur car sessionStore est reset
    private boolean setCookie = false;
    private String newSessionId = null;
    private List<char[]> game;
    private String bomb;
    private String flag;
    private boolean gzipCompression = false;

    public ThreadWorker(Socket s){
        try {
            this.socket = s;
            this.outputStream = s.getOutputStream();
            this.requestStream = s.getInputStream();
            this.responseStream = new PrintWriter(s.getOutputStream(), false);
        } 
        catch (Exception e) {
            System.err.println("error in the ThreadWorker constructor : " + e);
        }
        
    }

    @Override
    public void run(){
        try {
            //System.out.println("client connecté");

            bomb = base64Encoder("bomb.png");
            flag = base64Encoder("flag.png");

            while(true){
                List<String> fullRequest = getClientRequest();//ok ca marche

                if(fullRequest == null){
                    break;
                }

                System.out.println("requete : " + fullRequest.get(0));
                //System.out.println("headers : " + fullRequest.get(1));
                System.out.println("body : " + fullRequest.get(2));
                //System.out.println("cookie : " + fullRequest.get(3));
                //System.out.println("wsKey : " + fullRequest.get(4));

                
                String sessionID;
                if(!fullRequest.get(3).isEmpty()){//recupere la session en fct du cookie recu
                    String cookie = fullRequest.get(3);
                    if(!cookie.isEmpty()){
                        String[] cookieInfo = cookie.split(":");
                        for(String part : cookieInfo[1].split(";")){
                            if(part.startsWith("SESSID="))
                            sessionID = part.substring(7).trim();
                        }
                    }

                    if(sessionID != null)
                        session =sessionStore.get(sessionID);
                    /*sessionID = fullRequest.get(3).split("=")[1].trim();
                    session = sessionStore.get(sessionID);*/
                }


                if(fullRequest.get(0).startsWith("GET")){
                    if(fullRequest.get(0).contains("/webSocket")){
                        String clientKey = fullRequest.get(4).split(":")[1].trim();
                        handleWebSocket(clientKey);
                        break;//plus besoin de la connection vu qu on a le websocket
                    }
                    else handleGetRequest(fullRequest);
                }
                else if(fullRequest.get(0).startsWith("POST"))
                    handlePostRequest(fullRequest);
                else{// en theorie pas besoin car seulement de GET et POST depuis un navigateur
                    sendHttpResponse(405, "Method Not Allowed", "<html><body><h1>405 - Method Not Allowed</h1><p>The method " + fullRequest.get(0) + " is not allowed.</p></body></html>", fullRequest.get(3));
                    break;
                }
                //System.out.println("fin du run");
                //System.out.println();
            }
        }
        catch (Exception e) {
            System.err.println("error in run method : " + e);
        }
        finally {//fermeture des Stream et du socket
            try {
                if(requestStream != null) requestStream.close();
                if(responseStream != null) responseStream.close();
                if(socket != null && !socket.isClosed()) socket.close();
            } 
            catch (IOException e){
                System.err.println(e);
            }
        }
    }

    //la fonction va récupérer la requete, le header et le body si il y en a un et les renvoyer dans une liste 
    //de String contenant en position 0 la requete, en position 1 le header et en position 2 le body si il y'
    //en a un, en 3 le cookie du client et en 4 la cle websocket si il y en a une.
    private List<String> getClientRequest(){
        String line;
        String header = "";
        String requestLine = "";
        String body = "";
        String cookieLine = "";
        String wsKeyLine = "";
        int contentLength = 0;

        try {
            BufferedReader inputRequest = new BufferedReader(new InputStreamReader(requestStream));
            requestLine = inputRequest.readLine();
        
            if(requestLine == null){
                //System.out.println("requete non existante");
                return null;
            }
            else{
                //on récupère le header
                while((line = inputRequest.readLine()) != null && !line.isEmpty()){
                    if(line.toLowerCase().startsWith("cookie:")){// si il y a un cookie on le note
                        cookieLine = line;
                    }
                    if(line.toLowerCase().startsWith("sec-websocket-key")){//on note la clientKey du websocket
                        wsKeyLine = line;
                    }
                    if(line.toLowerCase().startsWith("accept-encoding")){//on veriie si on a gzip eneable
                        String[] encodingMods = line.split(":")[1].split(",");
                        for(int i = 0; i < encodingMods.length; i++){
                            if(encodingMods[i].trim().equalsIgnoreCase("gzip")){//ignorecase si le client envoie GZIP au lieu de gzip
                                gzipCompression = true;
                            }
                        }
                    }
                    header += line + "\n";

                    if(line.toLowerCase().startsWith("content-length")){
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }
                //on recupère le body si il y en a un
                if (contentLength > 0) {
                    char[] bodyChars = new char[contentLength];
                    inputRequest.read(bodyChars, 0, contentLength);
                    body += new String (bodyChars);
                }
            }
            
        } catch (Exception e) {
            System.err.println("error in getClientRequest method : " + e);
        }

        return Arrays.asList(requestLine, header, body, cookieLine, wsKeyLine);
        
    }
    /*la fonction va prendre en argument une liste de String dans laquelle le
     * premier argument est la premier ligne de la requete http, le deuxieme est 
     * les headers et le troisieme est le body si il y en a un.
     */
    private void handleGetRequest(List<String> fullRequest){
        if(fullRequest.get(0).equals("GET /play.html HTTP/1.1")){
            //HTTP/1.1 200 OK
            //generer la page play.html
            playHTML page = new playHTML();
            sendHttpResponse(200, "OK", page.generatePlayHTML(session, bomb, flag), fullRequest.get(3));
        }
        else if(fullRequest.get(0).equals("GET /leaderboard.html HTTP/1.1")){
            //HTTP/1.1 200 OK
            //generer la page leaderboard.html
            leaderboardHTML page = new leaderboardHTML();
            sendHttpResponse(200, "OK", page.generateLeaderboardHTML(finishedGame), fullRequest.get(3));

        }
        else if(fullRequest.get(0).equals("GET / HTTP/1.1")){
            //HTTP/1.1 303 See Other
            //Location: http://localhost:8014/play.html
            redirectionResponse("http://localhost:8014/play.html");
        }
        else{
            //erreur pas possible
            sendHttpResponse(404, "Not Found", "<html><body><h1>404 - Page Not Found</h1><p>The requested URL was not found on this server.</p></body></html>", fullRequest.get(3));
        }
    }

    /*la fonction va prendre en argument une liste de string comprenant une requet HTTP
     * POST et la traiter en fonction de ce quelle demande
     */
    private void handlePostRequest(List<String> fullRequest){
        if(fullRequest.get(0).equals("POST /set_username HTTP/1.1")){
            session.setPlayer(fullRequest.get(2).split("=")[1].trim());//on recupere bien le joueur
            //System.out.println(session.getPlayer());
            //playHTML page = new playHTML();
            //sendHttpResponse(200, "OK", page.generatePlayHTML(session), fullRequest.get(3));

            redirectionResponse("http://localhost:8014/play.html");
        }
        if(fullRequest.get(0).equals("POST /request HTTP/1.1")){

            String[] requestFrag = fullRequest.get(2).split("&");
            String[] action = requestFrag[0].split("=");
            String[] row = requestFrag[1].split("=");
            String[] col = requestFrag[2].split("=");
            String correctRequest = action[1].trim() + " " + row[1].trim() + " " + col[1].trim();

            if(correctRequest.startsWith("TRY")){
                handlePostTryRequest(correctRequest, fullRequest.get(3));
            }
            else if(correctRequest.startsWith("FLAG")){
                handlePostFlagRequest(correctRequest, fullRequest.get(3));
            }
            else{
                //erreur a gerer ici
            }
        }
    }
    /*la fonction va prendre le code de status, le massage de statu et le contenu 
     * que je veux avoir dans ma reponse et envoyer cette reponse au client
     * dans le format HTTP/1.1 
     */
    private void sendHttpResponse(int statusCode, String statusMessage, String content, String cookie){
        try{
            byte[] message = content.getBytes();
            StringBuilder http = new StringBuilder();
            http.append("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
            if(cookie.equals("")){
                //System.out.println("pas de cookie trouver");
                if(!firstLaunch){
                    http.append("Set-Cookie: SESSID=" + randomCookie() + "; Max-Age=3600; path=/; HttpOnly" + "\r\n");
                }
            }
            //la ligne juste en dessous c est pour reset le cookie si besoin
            if(firstLaunch){
                http.append("Set-Cookie: SESSID=; Max-Age=0; path=/; HttpOnly" + "\r\n");
                firstLaunch = false;
            }
            http.append("Content-Type: text/html" + "\r\n");
            http.append("Transfer-Encoding: chunked" + "\r\n");

            if(gzipCompression){
                http.append("Content-Encoding : gzip" + "\r\n");
                message = gzipCompress(message);
            }
            http.append("\r\n");

            outputStream.write(http.toString().getBytes("UTF-8"));
            outputStream.flush();

            sendChunks(message);
            responseStream.flush();
        }
        catch(Exception e){
            System.err.println(e);
        }
    }
    /*la fonction va prendre en arguments l'url de la location ou
     * l'on veux rediriger le client et envoyer une reponse HTTP/1.1 au client
     * contenant cette redirection 
     */
    private void redirectionResponse(String location){
        responseStream.println("HTTP/1.1 303 See Other");
        responseStream.println("Location: " + location);
        responseStream.println("Content-Length : 0");
        responseStream.println();
        responseStream.flush();
    }
    /*cette fonction va renvoyer un cookie random qui n est pas encore dans notre map */
    private String randomCookie(){
        String cookie = "";
        do{
            cookie = UUID.randomUUID().toString();//pour ne pas avoir un cookie qu on a deja dans notre map
        } while(sessionStore.containsKey(cookie));
        
        sessionStore.put(cookie, new Session());
        System.out.println("cookie cree: " + cookie);
        return cookie;
    }

    /*cette fonction va prendre en argument la cle du client qui a demande une upgrade websocket
     * il va transformer cette cle en cle serveur et renvoyer une reponse HTTP pour dire qu il accept l upgrade
     * elle va ensuite boucler en continue pour gere la communication entre le client et le serveur
     */
    private void handleWebSocket(String wsKey){
        //System.out.println("handling ws");
        try{
            //System.out.println("client key : " + wsKey);
            String serverKey = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                                    .digest((wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")));

            //System.out.println("serverKey : " + serverKey);
            responseStream.println("HTTP/1.1 101 Switching Protocols");
            responseStream.println("Connection: Upgrade");
            responseStream.println("Upgrade: websocket");
            responseStream.println("Sec-WebSocket-Accept: " + serverKey);
            responseStream.println();
            responseStream.flush();

            WebSocket webSocket = new WebSocket(socket);

            while(true){
                String clientRequest = webSocket.receive();

                if(clientRequest == null)
                    break;

                int gameState = handleClientRequest(clientRequest, webSocket);
                if(gameState == -1)
                    break;

            }
        }
        catch(Exception e){
            System.err.println(e);
        }

    }

    /*cette fonction prends en argument la requete client et le websocket utiliser pour communiquer avec celui ci
     * et va traiter cette requete en fonction des differents resultats possible
     * La fonction return -1 si le jeu est terminer 0 sinon
     */
    private int handleClientRequest(String clientRequest, WebSocket webSocket){
        if(clientRequest.startsWith("TRY")){
            int gameState = handleTryRequest(clientRequest, webSocket);
            if(gameState == -1){
                return -1;
            }
        }
        else if(clientRequest.startsWith("FLAG")){
            handleFlagRequest(clientRequest, webSocket);
            return 0;
        }
        else{
            handleWrongRequest("WRONG", webSocket);
            return 0;
        }
        return 0;
    }

    //cette fonction va gerer les requete de type TRY, elle prends comme argument
    //la requete et le websocket par lequel envoyer la reponse
    //elle renvoie - 1 si la partie est terminer 0 sinon
    private int handleTryRequest(String request, WebSocket webSocket){
        try{
            String[] splitedRequest = request.split(" ");

            //verifie que la requete est conforme
            if(splitedRequest.length != 3){
                handleWrongRequest("WRONG", webSocket);
            }

            //si row ou col est pas un nombre on a une numberformatexcpetion gerer dans le catch
            int row = Integer.parseInt(splitedRequest[1]);
            int col = Integer.parseInt(splitedRequest[2]);

            if(row > 6 || col >6){
                handleWrongRequest("INVALID RANGE", webSocket);
                return 0;
            }

            GameManager gameManager = new GameManager(session.getGame());
            gameManager.Try(row, col);

            if(gameManager.getGameState() == 1){
                gameManager.cheat();
                game = gameManager.getGame();
                session.setGame(game);
                sendGrid(game, "GAME WON", webSocket);
                session.endGame();
                finishedGame.add(session);
                return -1;
            }
            else if(gameManager.getGameState() == -1){
                gameManager.cheat();
                game = gameManager.getGame();
                session.setGame(game);
                sendGrid(game, "GAME LOST", webSocket);
                session.endGame();
                return -1;
            }
            game = gameManager.getGame();
            session.setGame(game);
            sendGrid(game, null, webSocket);
            return 0;

        }
        catch(NumberFormatException e){
            handleWrongRequest("col and row have to be NUMBERS", webSocket);
            return 0;
        }
        catch(Exception e){
            System.err.println("error in handleTryRequest() : " + e);
            return 0;
        }
    }

    /* cette fonction va traiter les requetes de type FLAG, elle prends en arguments
     * la requete et le websocket par lequel envoyer la reponse
     */
    private void handleFlagRequest(String request, WebSocket webSocket){
        try{
            if(session.getGame() == null){
                handleWrongRequest("GAME HAS NOT STARTED SO CAN'T FLAG", webSocket);
                return;
            }
            String[] splitedRequest = request.split(" ");

            //verifie que la requete FLAG est conforme
            if(splitedRequest.length != 3){
                handleWrongRequest("INVALID TRY SYNTAX", webSocket);
            }

            //si row ou col est pas un nombre on a une numberformatexcpetion gerer dans le catch
            int row = Integer.parseInt(splitedRequest[1]);
            int col = Integer.parseInt(splitedRequest[2]);

            if(row > 6 || col > 6){
                handleWrongRequest("INVALID RANGE", webSocket);
            }

            GameManager gameManager = new GameManager(session.getGame());
            gameManager.Flag(row, col);

            game = gameManager.getGame();
            session.setGame(game);
            sendGrid(game, null, webSocket);
            return;

        }
        catch(NumberFormatException e){
            handleWrongRequest("col and row have to be NUMBERS", webSocket);
            return;
        }
    }

    //cette fonction va gerer toutes les requete fausse en imprimant le message donné
    private void handleWrongRequest(String message, WebSocket webSocket){
        try{
            webSocket.send(message);
        }
        catch(IOException e){
            System.err.println(e);
        }
        return;
    }

    /*cette fonction prends en argument une partie, un message et le websocket par lequel envoyer 
     * la repones, la fonction va envoyer le game + le message si i y en a un a travers le websocket
     */
    private void sendGrid(List<char[]> game, String message, WebSocket webSocket){
        try{
            if(game == null){
                return;
            }

            StringBuilder string = new StringBuilder();

            for(int i = 0; i < 49; i++){
                if(game.get(i)[1] == 'U'){
                    string.append('#');
                }
                if(game.get(i)[1] == 'R'){
                    string.append(game.get(i)[0]);
                }
                if(game.get(i)[1] == 'F'){
                    string.append('F');
                }
                if((i+1) % 7 == 0){
                    string.append("\r\n");
                }
            }
            if(message != null){
                string.append(message);
            }
            string.append("\r\n");
            webSocket.send(string.toString());
        }
        catch(Exception e){
            System.err.println(e);
        }
        return;
    }

    /*cette fonction prends en arguments un string (path vers une image) et va 
     * transformer cette image en base64.
     */
    private String base64Encoder(String image){
        try{
            BufferedImage img = ImageIO.read(new File(image));
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "png", os);
            String base64Image = Base64.getEncoder().encodeToString(os.toByteArray());
            return base64Image;
        }
        catch(Exception e){
            System.err.println(e);
            return null;
        }
    }

    //ici pas de de PrtinWriter car ca corromp les donnes, celle ci doivent etre des bytes
    //et print writer envoie des string
    /*cette fonction prends en argument un tableau de byte et va envoyer ce tableau en suivant
     * le processus de chunk encoding au client via le outputStream
    */
    private void sendChunks(byte[] message){
        int maxChunkSize = 128;
        int offset = 0;
        try{
            while(offset < message.length){
                int chunkSize = Math.min(maxChunkSize, message.length - offset);
                String chunkHeader = Integer.toHexString(chunkSize) + "\r\n";

                outputStream.write(chunkHeader.getBytes("UTF-8"));
                outputStream.write(message, offset, chunkSize);
                outputStream.write("\r\n".getBytes("UTF-8"));

                offset += chunkSize;
            }
            outputStream.write("0\r\n\r\n".getBytes("UTF-8"));
            outputStream.flush();
        }
        catch(Exception e){
            System.err.println(e);
        }
    }

    /*cette fonction prends en argument un tableau de byte et renvoie un tableau de byte 
     * qui est celui en argument mais compresser grace a gzip
     */
    private byte[] gzipCompress(byte[] message){
        try{
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
            gzipStream.write(message);
            gzipStream.close();

            return byteStream.toByteArray();
        }
        catch(Exception e){
            System.err.println(e);
            return null;
        }
    }

    private void handlePostTryRequest(String request, String cookie){
        try{
            String[] splitedRequest = request.split(" ");

            //si row ou col est pas un nombre on a une numberformatexcpetion gerer dans le catch
            int row = Integer.parseInt(splitedRequest[1]);
            int col = Integer.parseInt(splitedRequest[2]);

            GameManager gameManager = new GameManager(session.getGame());
            gameManager.Try(row, col);
            

            game = gameManager.getGame();
            session.setGame(game);
            if(gameManager.getGameState() == 1){
                //à appronfondir
                session.endGame();
                finishedGame.add(session);
            }

            playHTML page = new playHTML();
            //sendHttpResponse(200, "OK", page.generatePlayHTML(session, bomb, flag), cookie);
            redirectionResponse("http://localhost:8014/play.html");
            return ;
        }
        catch(NumberFormatException e){
            return ;
        }
    }
    private void handlePostFlagRequest(String request, String cookie){
        try{
            String[] splitedRequest = request.split(" ");

            //si row ou col est pas un nombre on a une numberformatexcpetion gerer dans le catch
            int row = Integer.parseInt(splitedRequest[1]);
            int col = Integer.parseInt(splitedRequest[2]);

            GameManager gameManager = new GameManager(session.getGame());
            gameManager.Flag(row, col);

            game = gameManager.getGame();
            session.setGame(game);

            playHTML page = new playHTML();
            //sendHttpResponse(200, "OK", page.generatePlayHTML(session, bomb, flag), cookie);
            redirectionResponse("http://localhost:8014/play.html");
            return;

        }
        catch(NumberFormatException e){
            return;
        }
    }

    
}

