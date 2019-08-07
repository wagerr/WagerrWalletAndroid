package com.wagerrwallet.presenter.entities;


import com.platform.entities.TxMetaData;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 *
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

// BetEventEntity was created as DB entity and as UI holder entity
public class EventTxUiHolder extends BetEventEntity {
    public static final String TAG = EventTxUiHolder.class.getName();

    private EventTxUiHolder() {
    }

    // extended constructor with support text for UI
    public EventTxUiHolder(String txHash, BetTxType type, long version,
                          long eventID, long eventTimestamp, long sportID, long tournamentID, long roundID,
                          long homeTeamID, long awayTeamID, long homeOdds, long awayOdds, long drawOdds,
                          long entryPrice, long spreadPoints, long totalPoints, long overOdds, long underOdds,
                          long blockheight, long timestamp, String iso,
                          String txSport, String txTournament, String txRound, String txHomeTeam, String txAwayTeam,    // mappings
                          BetResultEntity.BetResultType resultType, long homeScore, long awayScore) {

        super( txHash, type, version, eventID, eventTimestamp, sportID, tournamentID, roundID,
                homeTeamID,  awayTeamID, homeOdds, awayOdds, drawOdds,
                entryPrice,  spreadPoints, totalPoints, overOdds, underOdds,
                blockheight, timestamp, iso );

        this.txSport = txSport;
        this.txTournament = txTournament;
        this.txRound = txRound;
        this.txHomeTeam = txHomeTeam;
        this.txAwayTeam = txAwayTeam;

        this.resultType = resultType;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

}
