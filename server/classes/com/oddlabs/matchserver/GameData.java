package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.Profile;

import java.sql.Timestamp;

public final class GameData {

    // Basic game information
    private Timestamp timeCreate;
    private String name;
    private String rated;
    private Integer speed;
    private Integer size;
    private Integer hills;
    private Integer trees;
    private Integer resources;
    private String mapcode;
    private String status;
    private Integer id;
    private Integer winner;
    private Timestamp timeStop;
    private Timestamp timeStart;

    // Player 1
    private String player1Name;
    private String player1Race;
    private Integer player1Team;
    private Profile profile1;

    // Player 2
    private String player2Name;
    private String player2Race;
    private Integer player2Team;
    private Profile profile2;

    // Player 3
    private String player3Name;
    private String player3Race;
    private Integer player3Team;
    private Profile profile3;

    // Player 4
    private String player4Name;
    private String player4Race;
    private Integer player4Team;
    private Profile profile4;

    // Player 5
    private String player5Name;
    private String player5Race;
    private Integer player5Team;
    private Profile profile5;

    // Player 6
    private String player6Name;
    private String player6Race;
    private Integer player6Team;
    private Profile profile6;

    // Player 7
    private String player7Name;
    private String player7Race;
    private Integer player7Team;
    private Profile profile7;

    // Player 8
    private String player8Name;
    private String player8Race;
    private Integer player8Team;
    private Profile profile8;

    // Constructor
    public GameData() {}

    // Getters and Setters for basic game information
    public Timestamp getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(Timestamp timeCreate) {
        this.timeCreate = timeCreate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRated() {
        return rated;
    }

    public void setRated(String rated) {
        this.rated = rated;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getHills() {
        return hills;
    }

    public void setHills(Integer hills) {
        this.hills = hills;
    }

    public Integer getTrees() {
        return trees;
    }

    public void setTrees(Integer trees) {
        this.trees = trees;
    }

    public Integer getResources() {
        return resources;
    }

    public void setResources(Integer resources) {
        this.resources = resources;
    }

    public String getMapcode() {
        return mapcode;
    }

    public void setMapcode(String mapcode) {
        this.mapcode = mapcode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWinner() {
        return winner;
    }

    public void setWinner(Integer winner) {
        this.winner = winner;
    }

    public Timestamp getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(Timestamp timeStop) {
        this.timeStop = timeStop;
    }

    public Timestamp getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Timestamp timeStart) {
        this.timeStart = timeStart;
    }

    // Player 1 getters and setters
    public String getPlayer1Name() {
        return player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }

    public String getPlayer1Race() {
        return player1Race;
    }

    public void setPlayer1Race(String player1Race) {
        this.player1Race = player1Race;
    }

    public Integer getPlayer1Team() {
        return player1Team;
    }

    public void setPlayer1Team(Integer player1Team) {
        this.player1Team = player1Team;
    }

    public Profile getProfile1() {
        return profile1;
    }

    public void setProfile1(Profile profile1) {
        this.profile1 = profile1;
    }

    // Player 2 getters and setters
    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    public String getPlayer2Race() {
        return player2Race;
    }

    public void setPlayer2Race(String player2Race) {
        this.player2Race = player2Race;
    }

    public Integer getPlayer2Team() {
        return player2Team;
    }

    public void setPlayer2Team(Integer player2Team) {
        this.player2Team = player2Team;
    }

    public Profile getProfile2() {
        return profile2;
    }

    public void setProfile2(Profile profile2) {
        this.profile2 = profile2;
    }

    // Player 3 getters and setters
    public String getPlayer3Name() {
        return player3Name;
    }

    public void setPlayer3Name(String player3Name) {
        this.player3Name = player3Name;
    }

    public String getPlayer3Race() {
        return player3Race;
    }

    public void setPlayer3Race(String player3Race) {
        this.player3Race = player3Race;
    }

    public Integer getPlayer3Team() {
        return player3Team;
    }

    public void setPlayer3Team(Integer player3Team) {
        this.player3Team = player3Team;
    }

    public Profile getProfile3() {
        return profile3;
    }

    public void setProfile3(Profile profile3) {
        this.profile3 = profile3;
    }

    // Player 4 getters and setters
    public String getPlayer4Name() {
        return player4Name;
    }

    public void setPlayer4Name(String player4Name) {
        this.player4Name = player4Name;
    }

    public String getPlayer4Race() {
        return player4Race;
    }

    public void setPlayer4Race(String player4Race) {
        this.player4Race = player4Race;
    }

    public Integer getPlayer4Team() {
        return player4Team;
    }

    public void setPlayer4Team(Integer player4Team) {
        this.player4Team = player4Team;
    }

    public Profile getProfile4() {
        return profile4;
    }

    public void setProfile4(Profile profile4) {
        this.profile4 = profile4;
    }

    // Player 5 getters and setters
    public String getPlayer5Name() {
        return player5Name;
    }

    public void setPlayer5Name(String player5Name) {
        this.player5Name = player5Name;
    }

    public String getPlayer5Race() {
        return player5Race;
    }

    public void setPlayer5Race(String player5Race) {
        this.player5Race = player5Race;
    }

    public Integer getPlayer5Team() {
        return player5Team;
    }

    public void setPlayer5Team(Integer player5Team) {
        this.player5Team = player5Team;
    }

    public Profile getProfile5() {
        return profile5;
    }

    public void setProfile5(Profile profile5) {
        this.profile5 = profile5;
    }

    // Player 6 getters and setters
    public String getPlayer6Name() {
        return player6Name;
    }

    public void setPlayer6Name(String player6Name) {
        this.player6Name = player6Name;
    }

    public String getPlayer6Race() {
        return player6Race;
    }

    public void setPlayer6Race(String player6Race) {
        this.player6Race = player6Race;
    }

    public Integer getPlayer6Team() {
        return player6Team;
    }

    public void setPlayer6Team(Integer player6Team) {
        this.player6Team = player6Team;
    }

    public Profile getProfile6() {
        return profile6;
    }

    public void setProfile6(Profile profile6) {
        this.profile6 = profile6;
    }

    // Player 7 getters and setters
    public String getPlayer7Name() {
        return player7Name;
    }

    public void setPlayer7Name(String player7Name) {
        this.player7Name = player7Name;
    }

    public String getPlayer7Race() {
        return player7Race;
    }

    public void setPlayer7Race(String player7Race) {
        this.player7Race = player7Race;
    }

    public Integer getPlayer7Team() {
        return player7Team;
    }

    public void setPlayer7Team(Integer player7Team) {
        this.player7Team = player7Team;
    }

    public Profile getProfile7() {
        return profile7;
    }

    public void setProfile7(Profile profile7) {
        this.profile7 = profile7;
    }

    // Player 8 getters and setters
    public String getPlayer8Name() {
        return player8Name;
    }

    public void setPlayer8Name(String player8Name) {
        this.player8Name = player8Name;
    }

    public String getPlayer8Race() {
        return player8Race;
    }

    public void setPlayer8Race(String player8Race) {
        this.player8Race = player8Race;
    }

    public Integer getPlayer8Team() {
        return player8Team;
    }

    public void setPlayer8Team(Integer player8Team) {
        this.player8Team = player8Team;
    }

    public Profile getProfile8() {
        return profile8;
    }

    public void setProfile8(Profile profile8) {
        this.profile8 = profile8;
    }
}
