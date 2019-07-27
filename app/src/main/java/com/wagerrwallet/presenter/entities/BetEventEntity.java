package com.wagerrwallet.presenter.entities;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 *
 * (c) Wagerr Betting platform 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BetEventEntity {
    public static final String TAG = BetEventEntity.class.getName();

    public enum BetEventType {
        PEERLESS(0x02),
        CHAIN_LOTTO(0x06),
        PEERLESS_SPREAD(0x09),
        PEERLESS_TOTAL(0x0a),
        UNKNOWN(-1);

        private int type;
        BetEventType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetEventType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetEventType eventType : BetEventType.values())
                if (eventType.type == value)
                    return eventType;
            return UNKNOWN;
        }
    }

    protected long blockheight;
    protected long timestamp;
    protected String txHash;
    protected String txISO;
    protected long version;
    protected BetEventType type;
    protected long eventID;
    protected long eventTimestamp;
    protected long sportID;
    protected long tournamentID;
    protected long roundID;
    protected String txSport;
    protected String txTournament;
    protected String txRound;
    protected long homeTeamID;
    protected String txHomeTeam;
    protected long awayTeamID;
    protected String txAwayTeam;
    protected long homeOdds;
    protected long awayOdds;
    protected long drawOdds;
    protected String txEvent;
    protected long entryPrice;
    protected long spreadPoints;
    protected long totalPoints;
    protected long overOdds;
    protected long underOdds;

    // constructor for DB
    public BetEventEntity(String txHash, BetEventType type, long version,
                          long eventID, long eventTimestamp, long sportID, long tournamentID, long roundID,
                          long homeTeamID, long awayTeamID, long homeOdds, long awayOdds, long drawOdds,
                          long entryPrice, long spreadPoints, long totalPoints, long overOdds, long underOdds,
                          long blockheight, long timestamp, String iso) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.txHash = txHash;
        this.txISO = iso;

        this.version = version;
        this.type = type;
        this.eventID = eventID;

        this.eventTimestamp = eventTimestamp;
        this.homeTeamID = homeTeamID;
        this.awayTeamID = awayTeamID;
        this.homeOdds = homeOdds;
        this.awayOdds = awayOdds;
        this.drawOdds = drawOdds;
        this.entryPrice = entryPrice;
        this.spreadPoints = spreadPoints;
        this.totalPoints = totalPoints;
        this.overOdds = overOdds;
        this.underOdds = underOdds;
    }

    // extended constructor with support text for UI
    public BetEventEntity(String txHash, BetEventType type, long version,
                          long eventID, long eventTimestamp, long sportID, long tournamentID, long roundID,
                          long homeTeamID, long awayTeamID, long homeOdds, long awayOdds, long drawOdds,
                          long entryPrice, long spreadPoints, long totalPoints, long overOdds, long underOdds,
                          long blockheight, long timestamp, String iso,
                          String txEvent, String txSport, String txTournament, String txRound, String txHomeTeam, String txAwayTeam) {

        this( txHash, type, version, eventID, eventTimestamp, sportID, tournamentID, roundID,
                homeTeamID,  awayTeamID, homeOdds, awayOdds, drawOdds,
                entryPrice,  spreadPoints, totalPoints, overOdds, underOdds,
                blockheight, timestamp, iso );

        this.txEvent = txEvent;
        this.txSport = txSport;
        this.txTournament = txTournament;
        this.txRound = txRound;
        this.txHomeTeam = txHomeTeam;
        this.txAwayTeam = txAwayTeam;
    }

    protected BetEventEntity() {
    }

    public String getTxISO() {
        return txISO;
    }

    public long getVersion() {
        return version;
    }

    public BetEventType getType() {
        return type;
    }

    public long getSportID() {
        return sportID;
    }

    public long getTournamentID() {
        return tournamentID;
    }

    public long getRoundID() {
        return roundID;
    }

    public String getTxSport() {
        return txSport;
    }

    public String getTxTournament() {
        return txTournament;
    }

    public String getTxRound() {
        return txRound;
    }

    public String getTxHomeTeam() {
        return txHomeTeam;
    }

    public String getTxAwayTeam() {
        return txAwayTeam;
    }

    public String getTxEvent() {
        return txEvent;
    }

    public long getEntryPrice() {
        return entryPrice;
    }

    public long getSpreadPoints() {
        return spreadPoints;
    }

    public long getTotalPoints() {
        return totalPoints;
    }

    public long getOverOdds() {
        return overOdds;
    }

    public long getUnderOdds() {
        return underOdds;
    }

    public long getBlockheight() {
        return blockheight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getEventDescription() {
        return txEvent;
    }

    public String getSportDescription(){ return txSport; }

    public String getTournamentDescription() { return txTournament; }

    public long getEventID() {
        return eventID;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public String getRoundDescription() {
        return txRound;
    }

    public long getHomeTeamID() {
        return homeTeamID;
    }

    public String getHomeTeamDescription() {
        return txHomeTeam;
    }

    public long getAwayTeamID() {
        return awayTeamID;
    }

    public String getAwayTeamDescription() {
        return txAwayTeam;
    }

    public long getHomeOdds() {
        return homeOdds;
    }

    public long getAwayOdds() {
        return awayOdds;
    }

    public long getDrawOdds() {
        return drawOdds;
    }
}
