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
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLaserAMSTHB extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -1940059603781427515L;

    /**
     *
     */
    public ISLaserAMSTHB() {
        super();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "Laser AMS (THB)";
        setInternalName("ISLaserAntiMissileSystemTHB");
        addLookupName("IS Laser Anti-Missile System (THB)");
        addLookupName("IS Laser AMS (THB)");
        heat = 3;
        rackSize = 2;
        damage = 2; // # of d6 of missiles affected
        ammoType = AmmoType.T_NA;
        tonnage = 1.5f;
        criticals = 2;
        bv = 105;
        // we need to remove the direct fire flag again, so TC weight is not
        // affected
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_AUTO_TARGET).or(F_HEATASDICE).or(F_AMS)
                .and(F_DIRECT_FIRE.not());
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 300000;
        //Since this are the Tactical Handbook Weapons I'm using the TM Stats.
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3059;
        techLevel.put(3059, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
