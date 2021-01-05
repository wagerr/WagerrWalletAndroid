package com.wagerrwallet.presenter.entities;


import com.wagerrwallet.WagerrApp;
import com.wagerrwallet.presenter.activities.settings.BetSettings;
import com.wagerrwallet.tools.manager.BRSharedPrefs;

import java.text.DecimalFormat;

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
    public static final long ODDS_MULTIPLIER = 10000;
    public static final long SPREAD_MULTIPLIER = 100;
    public static final long TOTAL_MULTIPLIER = 100;
    public static final long RESULT_MULTIPLIER = 100;


    public enum BetTxType {
        PEERLESS(0x02),
        UPDATEODDS(0x05),
        CHAIN_LOTTO(0x06),
        PEERLESS_SPREAD(0x09),
        PEERLESS_TOTAL(0x0a),
        UNKNOWN(-1);

        private int type;
        BetTxType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetTxType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetTxType eventType : BetTxType.values())
                if (eventType.type == value)
                    return eventType;
            return UNKNOWN;
        }
    }

    // table data
    protected long blockheight;
    protected long timestamp;
    protected long lastUpdated;
    protected String txHash;
    protected String txISO;
    protected long version;
    protected BetTxType type;
    protected long eventID;
    protected long eventTimestamp;
    protected long sportID;
    protected long tournamentID;
    protected long roundID;
    protected long homeTeamID;
    protected long awayTeamID;
    protected long homeOdds;
    protected long awayOdds;
    protected long drawOdds;
    protected long entryPrice;
    protected long spreadPoints;
    protected long spreadHomeOdds;
    protected long spreadAwayOdds;
    protected long totalPoints;

    protected long overOdds;
    protected long underOdds;

    // mappings
    protected String txSport;
    protected String txTournament;
    protected String txRound;
    protected String txHomeTeam;
    protected String txAwayTeam;

    // results
    protected BetResultEntity.BetResultType resultType;
    protected long homeScore;
    protected long awayScore;

    // constructor for DB
    public BetEventEntity(String txHash, BetTxType type, long version,
                          long eventID, long eventTimestamp, long sportID, long tournamentID, long roundID,
                          long homeTeamID, long awayTeamID, long homeOdds, long awayOdds, long drawOdds,
                          long entryPrice, long spreadPoints, long spreadHomeOdds, long spreadAwayOdds,
                          long totalPoints, long overOdds, long underOdds,
                          long blockheight, long timestamp, String iso, long lastUpdated) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
        this.txHash = txHash;
        this.txISO = iso;

        this.version = version;
        this.type = type;
        this.eventID = eventID;
        this.sportID = sportID;
        this.roundID = roundID;
        this.tournamentID = tournamentID;

        this.eventTimestamp = eventTimestamp;
        this.homeTeamID = homeTeamID;
        this.awayTeamID = awayTeamID;
        this.homeOdds = homeOdds;
        this.awayOdds = awayOdds;
        this.drawOdds = drawOdds;
        this.entryPrice = entryPrice;
        this.spreadPoints = spreadPoints;
        this.spreadHomeOdds = spreadHomeOdds;
        this.spreadAwayOdds = spreadAwayOdds;
        this.totalPoints = totalPoints;
        this.overOdds = overOdds;
        this.underOdds = underOdds;
    }

    protected BetEventEntity() {
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

    public String getTxISO() {
        return txISO;
    }

    public long getVersion() {
        return version;
    }

    public BetTxType getType() {
        return type;
    }

    public long getEventID() {
        return eventID;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
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

    public long getHomeTeamID() {
        return homeTeamID;
    }

    public long getAwayTeamID() {
        return awayTeamID;
    }

    public String getOddTx( float odd)  {
        return BetEventEntity.getOddText( odd );
    }

    public static String getOddText( float odd)  {
        boolean settingAmerican = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_AMERICAN, false);
        DecimalFormat df;
        String ret;

        df = new DecimalFormat();
        df.setMaximumFractionDigits( (settingAmerican) ? 0 : 2 );
        df.setMinimumFractionDigits( (settingAmerican) ? 0 : 2 );
        df.setPositivePrefix( (settingAmerican) ? "+" : "");
        df.setNegativePrefix("-");

        ret = df.format(odd);

        if ( ret.equals("+100") )   {
            ret = "-100";
        }

        return ret;
    }

    public String getTxHomeOdds() {
        if (homeOdds==0)    return "N/A";
        else                return getOddTx(getOdds((float)homeOdds/ODDS_MULTIPLIER));
    }

    public String getTxAwayOdds() {
        if (awayOdds==0)    return "N/A";
        else                return getOddTx(getOdds((float)awayOdds/ODDS_MULTIPLIER));
    }

    public String getTxDrawOdds() {
        if (drawOdds==0)    return "N/A";
        else                return getOddTx(getOdds((float)drawOdds/ODDS_MULTIPLIER));
    }

    public static float getOddsStatic( float odds ) {
        boolean settingOdds = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_ODDS, false);
        boolean settingAmerican = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_AMERICAN, false);

        float ret = (settingOdds) ? odds : (float)((odds-1)*0.94)+1;
        ret = (settingAmerican) ? DecimalToAmerican(ret) : ret;

        return ret;
    }

    public float getOdds( float odds )  {
        return BetEventEntity.getOddsStatic( odds );
    }

    public static float DecimalToAmerican(float odd)   {
        if (odd>2)  {
            return (odd-1)*100;
        }
        else {
            return (-100) / (odd-1);
        }

    }

    public String getSpreadFormat() {
        // spreads v2
        return (getSpreadPoints()>0)?"+%s/-%s":"-%s/+%s";
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

    public long getEntryPrice() {
        return entryPrice;
    }

    public long getSpreadPoints() {
        return spreadPoints;
    }

    public long getSpreadHomeOdds() {
        return spreadHomeOdds;
    }

    public long getSpreadAwayOdds() {
        return spreadAwayOdds;
    }

    public String getTxSpreadPoints() {
        return BetEventEntity.getTextSpreadPoints( spreadPoints );
    }

    public static String getTextSpreadPoints( float spreadP ) {
        return String.valueOf((float)Math.abs(spreadP)/SPREAD_MULTIPLIER);
    }

    public String getTxSpreadHomeOdds() {
        if (spreadHomeOdds==0)      return "N/A";
        else                        return getOddTx(getOdds((float)spreadHomeOdds/ODDS_MULTIPLIER));
    }

    public String getTxSpreadAwayOdds() {
        if (spreadAwayOdds==0)    return "N/A";
        else                      return getOddTx(getOdds((float)spreadAwayOdds/ODDS_MULTIPLIER));
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

    public String getTxTotalPoints() {
        return BetEventEntity.getTextTotalPoints( totalPoints );
    }

    public static String getTextTotalPoints( float total ) {
        return String.valueOf((float)total/TOTAL_MULTIPLIER);
    }

    public String getTxOverOdds() {
        if (overOdds==0)    return "N/A";
        else                return getOddTx(getOdds((float)overOdds/ODDS_MULTIPLIER));
    }

    public String getTxUnderOdds() {
        if (underOdds==0)   return "N/A";
        else                return getOddTx(getOdds((float)underOdds/ODDS_MULTIPLIER));
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

    public BetResultEntity.BetResultType getResultType() {
        return resultType;
    }

    public long getHomeScore() {
        return homeScore;
    }

    public long getAwayScore() {
        return awayScore;
    }

    public String getTxAwayScore() {
        return (awayScore<0)?"N/A":String.valueOf(awayScore/RESULT_MULTIPLIER);
    }

    public String getTxHomeScore() {
        return (homeScore<0)?"N/A":String.valueOf(homeScore/RESULT_MULTIPLIER);
    }

    public String getTxHomeTeam() {
        return txHomeTeam;
    }

    public String getTxAwayTeam() {
        return txAwayTeam;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}
