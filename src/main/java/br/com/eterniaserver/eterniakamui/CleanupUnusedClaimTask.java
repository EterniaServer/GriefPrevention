/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package br.com.eterniaserver.eterniakamui;

import br.com.eterniaserver.eterniakamui.enums.Booleans;
import br.com.eterniaserver.eterniakamui.enums.Integers;
import br.com.eterniaserver.eterniakamui.events.ClaimExpirationEvent;
import br.com.eterniaserver.eterniakamui.enums.CustomLogEntryTypes;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

class CleanupUnusedClaimTask implements Runnable {
    final Claim claim;
    final PlayerData ownerData;
    final OfflinePlayer ownerInfo;

    CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo) {
        this.claim = claim;
        this.ownerData = ownerData;
        this.ownerInfo = ownerInfo;
    }

    @Override
    public void run() {


        //determine area of the default chest claim
        int areaOfDefaultClaim = 0;
        if (EterniaKamui.getInt(Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS) >= 0) {
            areaOfDefaultClaim = (int) Math.pow(EterniaKamui.getInt(Integers.CLAIMS_AUTOMATIC_CLAIMS_FOR_NEW_PLAYERS_RADIUS) * 2 + 1, 2);
        }

        //if this claim is a chest claim and those are set to expire
        if (claim.getArea() <= areaOfDefaultClaim && EterniaKamui.getInt(Integers.CLAIMS_CHEST_CLAIM_EXPIRATION_DAYS) > 0) {
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
            Calendar sevenDaysAgo = Calendar.getInstance();
            sevenDaysAgo.add(Calendar.DATE, - EterniaKamui.getInt(Integers.CLAIMS_CHEST_CLAIM_EXPIRATION_DAYS));
            boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastSeen()));
            if (newPlayerClaimsExpired && ownerData.getClaims().size() == 1) {
                if (expireEventCanceled())
                    return;
                claim.removeSurfaceFluids(null);
                EterniaKamui.instance.dataStore.deleteClaim(claim, true, true);

                //if configured to do so, restore the land to natural
                if (EterniaKamui.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) || EterniaKamui.getBool(Booleans.CLAIMS_SURVIVAL_AUTO_NATURE_RESTORATION)) {
                    EterniaKamui.instance.restoreClaim(claim, 0);
                }

                EterniaKamui.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
            }
        }

        //if configured to always remove claims after some inactivity period without exceptions...
        else if (EterniaKamui.getInt(Integers.CLAIMS_EXPIRATION_DAYS) > 0) {
            Calendar earliestPermissibleLastLogin = Calendar.getInstance();
            earliestPermissibleLastLogin.add(Calendar.DATE, - EterniaKamui.getInt(Integers.CLAIMS_EXPIRATION_DAYS));

            if (earliestPermissibleLastLogin.getTime().after(new Date(ownerInfo.getLastSeen()))) {
                if (expireEventCanceled()) {
                    return;
                }
                //make a copy of this player's claim list
                Vector<Claim> claims = new Vector<>(ownerData.getClaims());

                //delete them
                EterniaKamui.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
                EterniaKamui.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);
                EterniaKamui.AddLogEntry("earliestPermissibleLastLogin#getTime: " + earliestPermissibleLastLogin.getTime(), CustomLogEntryTypes.Debug, true);
                EterniaKamui.AddLogEntry("ownerInfo#getLastPlayed: " + ownerInfo.getLastSeen(), CustomLogEntryTypes.Debug, true);

                for (Claim value : claims) {
                    //if configured to do so, restore the land to natural
                    if (EterniaKamui.instance.creativeRulesApply(value.getLesserBoundaryCorner()) || EterniaKamui.getBool(Booleans.CLAIMS_SURVIVAL_AUTO_NATURE_RESTORATION)) {
                        EterniaKamui.instance.restoreClaim(value, 0);
                    }
                }
            }
        } else if (EterniaKamui.getInt(Integers.CLAIMS_UNUSED_CLAIM_EXPIRATION_DAYS) > 0 && EterniaKamui.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
            //avoid scanning large claims and administrative claims
            if (claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;

            //otherwise scan the claim content
            int minInvestment = 400;

            long investmentScore = claim.getPlayerInvestmentScore();

            if (investmentScore < minInvestment) {
                //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
                Calendar sevenDaysAgo = Calendar.getInstance();
                sevenDaysAgo.add(Calendar.DATE, - EterniaKamui.getInt(Integers.CLAIMS_UNUSED_CLAIM_EXPIRATION_DAYS));
                boolean claimExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastSeen()));
                if (claimExpired) {
                    if (expireEventCanceled())
                        return;
                    EterniaKamui.instance.dataStore.deleteClaim(claim, true, true);
                    EterniaKamui.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + EterniaKamui.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                    //restore the claim area to natural state
                    EterniaKamui.instance.restoreClaim(claim, 0);
                }
            }
        }
    }

    public boolean expireEventCanceled() {
        //see if any other plugins don't want this claim deleted
        ClaimExpirationEvent event = new ClaimExpirationEvent(this.claim);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
