package gg.feral.burrow.ui;
import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.Dimension;
import javax.swing.JToolTip;
import java.util.*;
/** Measurement of exact previous long-plain-text construction versus bounded preview. */
public final class PreparationTooltipSizeComparison {
  public static void main(String[] ignore) {
    BurrowModel m = new BurrowModel();
    int worstOld = 0, worstNew = 0;
    String worstBoss = "";
    for(String boss : VerifiedOutfitterCatalog.BOSSES) {
      m.chooseBoss(boss);
      for(String method : m.methods()) {
        m.chooseMethod(method);
        for (Tier tier : Tier.values()) {
          m.chooseTier(tier);
          var p=m.profile(); if(p==null) continue;
          var r=Final06ProfileMetadata.find(m.boss(),m.method(),m.tier());
          String old = String.join(" · ",p.notes());
          if(r!=null) old=old+" · Required unlocks: "+String.join("; ",r.questRequirements())
            +" · Charges and item states: "+String.join("; ",r.chargedStateRequirements())
            +" · Switches: "+String.join("; ",r.switches());
          String newText=PreparationTooltip.render(p.notes(),r==null?List.of():r.questRequirements(),r==null?List.of():r.chargedStateRequirements(),r==null?List.of():r.switches());
          JToolTip t=new JToolTip();t.setTipText(old);Dimension original=t.getPreferredSize();
          t.setTipText(newText);Dimension replacement=t.getPreferredSize();
          if(original.width>worstOld){worstOld=original.width;worstBoss=boss+" / "+method+" / "+tier;}
          worstNew=Math.max(worstNew,replacement.width);
        }
      }
    }
    System.out.println("PRIOR unwrapped max width="+worstOld+" px ("+worstBoss+")");
    System.out.println("PATCH bounded max width="+worstNew+" px");
  }
}
