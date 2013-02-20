/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Jason Tighe
 */
public class RifleWeaponHandler extends AmmoWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 7468287406174862534L;

    private HitData hit;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public RifleWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {

        double toReturn = wtype.getDamage();
        // we default to direct fire weapons for anti-infantry damage
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS() : 0, wtype.getInfantryDamageClass(), ((Infantry) target).isMechanized());
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity)target;
            hit = te.rollHitLocation(toHit.getHitTable(), toHit
                    .getSideTable(), waa.getAimedLocation(), waa.getAimingMode(), toHit.getCover());
            if (!(te instanceof BattleArmor) && !(te instanceof Infantry) && (!te.hasBARArmor(hit.getLocation()) || (te.getBARRating(hit.getLocation()) >= 8))) {
                toReturn = Math.max(0, toReturn - 3);
            }
        }

        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }

        if (game.getOptions().booleanOption("tacops_range") && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        return (int) toReturn;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        int nDamage;
        missed = false;

        hit.setGeneralDamageType(generalDamageType);
        hit.setBoxCars(roll == 12);

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            Report r = new Report(3460);
            r.subject = subjectId;
            r.add(entityTarget.getShortName());
            r.add(entityTarget.getLocationAbbr(hit));
            r.indent(2);
            vPhaseReport.addElement(r);
            nDamage = 0;
            missed = true;
            return;
        }

        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);

        // for non-salvo shots, report that the aimed shot was successfull
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            r = new Report(3410);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        if ( bDirect ){
            hit.makeDirectBlow(toHit.getMoS()/3);
        }
        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg, entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);

        //some buildings scale remaining damage that is not absorbed
        //TODO: this isn't quite right for castles brian
        if(null != bldg) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront, underWater, nukeS2S));
        }
    }
}
