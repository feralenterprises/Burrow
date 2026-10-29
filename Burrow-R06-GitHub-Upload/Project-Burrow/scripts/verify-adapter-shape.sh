#!/usr/bin/env bash
# A Java API-SHAPE compile, not a real RuneLite dependency compile.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/require-java17.sh
stub=build/host-api-stubs
mkdir -p "$stub/javax/inject" "$stub/net/runelite/client/plugins" "$stub/net/runelite/client/ui" "$stub/classes"
cat > "$stub/javax/inject/Inject.java" <<'EOF'
package javax.inject;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Inject { }
EOF
cat > "$stub/net/runelite/client/plugins/PluginDescriptor.java" <<'EOF'
package net.runelite.client.plugins;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface PluginDescriptor { String name(); String description() default ""; }
EOF
cat > "$stub/net/runelite/client/plugins/Plugin.java" <<'EOF'
package net.runelite.client.plugins;
public abstract class Plugin { protected void startUp() throws Exception {} protected void shutDown() throws Exception {} }
EOF
cat > "$stub/net/runelite/client/ui/PluginPanel.java" <<'EOF'
package net.runelite.client.ui;
public abstract class PluginPanel extends javax.swing.JPanel { protected PluginPanel() {} }
EOF
cat > "$stub/net/runelite/client/ui/ClientToolbar.java" <<'EOF'
package net.runelite.client.ui;
public class ClientToolbar { public void addNavigation(NavigationButton n) {} public void removeNavigation(NavigationButton n) {} }
EOF
cat > "$stub/net/runelite/client/ui/NavigationButton.java" <<'EOF'
package net.runelite.client.ui;
public class NavigationButton {
 public static Builder builder() { return new Builder(); }
 public static class Builder {
  public Builder tooltip(String s) {return this;}
  public Builder icon(java.awt.image.BufferedImage i) {return this;}
  public Builder priority(int p) {return this;}
  public Builder panel(PluginPanel p) {return this;}
  public NavigationButton build() {return new NavigationButton();}
 }
}
EOF
find "$stub" -name '*.java' | sort > build/HOST-STUB-SOURCES.txt
"$JAVAC_BIN" --release 17 -cp build/offline-main -d "$stub/classes" @build/HOST-STUB-SOURCES.txt src/main/java/gg/feral/burrow/runelite/BurrowPlugin.java
printf '%s\n' 'BURROW HOST API-SHAPE COMPILE PASS (LOCAL STUBS ONLY; REAL RUNELITE NOT TESTED)' | tee build/host-adapter-shape-result.txt
