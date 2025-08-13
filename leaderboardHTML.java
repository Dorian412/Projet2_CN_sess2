import java.util.*;

public class leaderboardHTML{
    public String generateLeaderboardHTML(List<Session> finishedGames){
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"fr\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Classement des meilleurs joueurs</title>\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: Arial, sans-serif;\n");
        html.append("            background-color: #f4f4f9;\n");
        html.append("            color: #333;\n");
        html.append("            margin: 0;\n");
        html.append("            padding: 20px;\n");
        html.append("        }\n");
        html.append("        table {\n");
        html.append("            width: 100%;\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            margin-top: 20px;\n");
        html.append("        }\n");
        html.append("        table, th, td {\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("        }\n");
        html.append("        th, td {\n");
        html.append("            padding: 8px 12px;\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        th {\n");
        html.append("            background-color: #4CAF50;\n");
        html.append("            color: white;\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        if(finishedGames == null){
            html.append("   <h1> Aucun score disponible</h1>");
        }
        else{
            html.append(getLearderBoard(finishedGames));
        }
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String getLearderBoard(List<Session> finishedGames){

        StringBuilder leaderBoard = new StringBuilder();
        List<Session> sortedGames = new ArrayList<>(finishedGames);
        sortedGames.sort(Comparator.comparingLong(Session::getDuration));
        leaderBoard.append("    <h1>Classement des meilleurs joueurs</h1>");
        leaderBoard.append("    <table>");
        leaderBoard.append("        <thead>");
        leaderBoard.append("            <tr>");
        leaderBoard.append("                <th scope=\"col\">Classement</th>");
        leaderBoard.append("                <th scope=\"col\">Joueur</th>");
        leaderBoard.append("                <th scope=\"col\">Score</th>");
        leaderBoard.append("            </tr>");
        leaderBoard.append("        </thead>");
        leaderBoard.append("        <tbody>");

        int classement = 0;
        synchronized (sortedGames) {
            
            for(Session game : sortedGames){
                ++classement;
                long second = (game.getDuration() / 1000) %60;
                long Minute = (game.getDuration() / 1000) /60;

                leaderBoard.append("            <tr>");
                String line = "            <td>"+classement+"</td><td>"+game.getPlayer()+"</td><td> "+String.format("%02d:%02d", Minute,second)+"</td>";
                leaderBoard.append(line);
                leaderBoard.append("            </tr>");

            }
        }
        if(classement == 0){
            leaderBoard.append("            <tr>");
            String line = "            <td>"+classement+"</td><td>"+"No player has won yet"+"</td><td> "+"00:00"+"</td>";
            leaderBoard.append(line);
            leaderBoard.append("            </tr>");
        }


        leaderBoard.append("        </tbody>");
        leaderBoard.append("    </table>");


        return leaderBoard.toString();
    }
}