package com.xueyin.genshinperf;

public class NetworkOptimizer {

    private static final String HEADER =
            "BACKUP=/data/local/tmp/net_boost_backup.conf\n" +
            "WIFI_IF=wlan0\n" +
            "CELL_IF=$(ip link show 2>/dev/null | grep ccmni | grep UP | grep -oE 'ccmni[0-9]+' | head -1)\n" +
            "[ -z \"$CELL_IF\" ] && CELL_IF=ccmni1\n";

    private static final String BACKUP_BLOCK =
            "if [ ! -f \"$BACKUP\" ]; then\n" +
            "  echo \"tcp_congestion=$(cat /proc/sys/net/ipv4/tcp_congestion_control)\" > \"$BACKUP\"\n" +
            "  echo \"tcp_rmem=\\\"$(cat /proc/sys/net/ipv4/tcp_rmem)\\\"\" >> \"$BACKUP\"\n" +
            "  echo \"tcp_wmem=\\\"$(cat /proc/sys/net/ipv4/tcp_wmem)\\\"\" >> \"$BACKUP\"\n" +
            "  echo \"rmem_max=$(cat /proc/sys/net/core/rmem_max)\" >> \"$BACKUP\"\n" +
            "  echo \"wmem_max=$(cat /proc/sys/net/core/wmem_max)\" >> \"$BACKUP\"\n" +
            "  echo \"tcp_slow_start=$(cat /proc/sys/net/ipv4/tcp_slow_start_after_idle)\" >> \"$BACKUP\"\n" +
            "  echo \"tcp_fastopen=$(cat /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null || echo 0)\" >> \"$BACKUP\"\n" +
            "  echo \"tcp_timestamps=$(cat /proc/sys/net/ipv4/tcp_timestamps)\" >> \"$BACKUP\"\n" +
            "  echo \"ipv6_disable=$(cat /proc/sys/net/ipv6/conf/all/disable_ipv6)\" >> \"$BACKUP\"\n" +
            "  echo \"[ BACKUP ] Default settings saved\"\n" +
            "fi\n";

    private static String tcpProfile(String mode) {
        StringBuilder s = new StringBuilder();
        switch (mode) {
            case "gaming":
                s.append("echo reno > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_no_metrics_save 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n");
                s.append("echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n");
                s.append("echo '87380 262144 1048576' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n");
                s.append("echo '87380 262144 1048576' > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n");
                s.append("echo 1048576 > /proc/sys/net/core/rmem_max 2>/dev/null\n");
                s.append("echo 1048576 > /proc/sys/net/core/wmem_max 2>/dev/null\n");
                s.append("echo 30 > /proc/sys/net/ipv4/tcp_keepalive_time 2>/dev/null\n");
                s.append("echo 5  > /proc/sys/net/ipv4/tcp_keepalive_intvl 2>/dev/null\n");
                s.append("echo 6  > /proc/sys/net/ipv4/tcp_keepalive_probes 2>/dev/null\n");
                s.append("echo 3000 > /proc/sys/net/core/netdev_max_backlog 2>/dev/null\n");
                s.append("echo \"[ TCP ] Profile gaming applied (reno, 1MB, ts=OFF)\"\n");
                break;
            case "download":
                s.append("echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_window_scaling 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n");
                s.append("echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n");
                s.append("echo '4096 87380 16777216' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n");
                s.append("echo '4096 65536 16777216' > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n");
                s.append("echo 16777216 > /proc/sys/net/core/rmem_max 2>/dev/null\n");
                s.append("echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null\n");
                s.append("echo '94500 125000 16777216' > /proc/sys/net/ipv4/tcp_mem 2>/dev/null\n");
                s.append("echo 5000 > /proc/sys/net/core/netdev_max_backlog 2>/dev/null\n");
                s.append("echo 15 > /proc/sys/net/ipv4/tcp_fin_timeout 2>/dev/null\n");
                s.append("echo \"[ TCP ] Profile download applied (cubic, 16MB)\"\n");
                break;
            case "streaming":
                s.append("echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_window_scaling 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n");
                s.append("echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n");
                s.append("echo '4096 87380 8388608' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n");
                s.append("echo '4096 65536 4194304' > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n");
                s.append("echo 8388608 > /proc/sys/net/core/rmem_max 2>/dev/null\n");
                s.append("echo 4194304 > /proc/sys/net/core/wmem_max 2>/dev/null\n");
                s.append("echo 300 > /proc/sys/net/ipv4/tcp_keepalive_time 2>/dev/null\n");
                s.append("echo 30  > /proc/sys/net/ipv4/tcp_keepalive_intvl 2>/dev/null\n");
                s.append("echo 9   > /proc/sys/net/ipv4/tcp_keepalive_probes 2>/dev/null\n");
                s.append("echo 4000 > /proc/sys/net/core/netdev_max_backlog 2>/dev/null\n");
                s.append("echo 30 > /proc/sys/net/ipv4/tcp_fin_timeout 2>/dev/null\n");
                s.append("echo \"[ TCP ] Profile streaming applied (cubic, 8MB)\"\n");
                break;
            case "upload":
                s.append("echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n");
                s.append("echo 0 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_sack 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_window_scaling 2>/dev/null\n");
                s.append("echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n");
                s.append("echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n");
                s.append("echo '4096 87380 4194304' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n");
                s.append("echo '4096 65536 16777216' > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n");
                s.append("echo 4194304 > /proc/sys/net/core/rmem_max 2>/dev/null\n");
                s.append("echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null\n");
                s.append("echo '94500 125000 16777216' > /proc/sys/net/ipv4/tcp_mem 2>/dev/null\n");
                s.append("echo 5000 > /proc/sys/net/core/netdev_max_backlog 2>/dev/null\n");
                s.append("echo 20 > /proc/sys/net/ipv4/tcp_fin_timeout 2>/dev/null\n");
                s.append("echo 65536 > /proc/sys/net/ipv4/tcp_max_orphans 2>/dev/null\n");
                s.append("echo \"[ TCP ] Profile upload applied (cubic, 4R/16W)\"\n");
                break;
        }
        return s.toString();
    }

    private static int qlenFor(String mode) {
        if ("gaming".equals(mode)) return 5000;
        if ("streaming".equals(mode)) return 8000;
        return 10000;
    }

    public static String wifiMode(String mode) {
        StringBuilder s = new StringBuilder();
        s.append(HEADER).append(BACKUP_BLOCK).append(tcpProfile(mode));
        if ("gaming".equals(mode)) s.append("ip -s -s neigh flush all > /dev/null 2>&1\n");
        String ipv6 = "gaming".equals(mode) ? "1" : "0";
        s.append("echo ").append(ipv6).append(" > /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null\n");
        s.append("echo ").append(ipv6).append(" > /proc/sys/net/ipv6/conf/$WIFI_IF/disable_ipv6 2>/dev/null\n");
        s.append("ip link set $WIFI_IF txqueuelen ").append(qlenFor(mode)).append(" 2>/dev/null\n");
        s.append("iw dev $WIFI_IF set power_save off 2>/dev/null\n");
        s.append("echo \"[ MODE ] WiFi ").append(mode).append(" - IPv6 ").append(ipv6.equals("1") ? "OFF" : "ON").append("\"\n");
        s.append("echo \"[ DONE ] Interface: $WIFI_IF\"\n");
        return s.toString();
    }

    public static String cellMode(String mode) {
        StringBuilder s = new StringBuilder();
        s.append(HEADER).append(BACKUP_BLOCK).append(tcpProfile(mode));
        if ("gaming".equals(mode)) s.append("ip -s -s neigh flush all > /dev/null 2>&1\n");
        String ipv6 = "gaming".equals(mode) ? "1" : "0";
        s.append("echo ").append(ipv6).append(" > /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null\n");
        s.append("echo ").append(ipv6).append(" > /proc/sys/net/ipv6/conf/$CELL_IF/disable_ipv6 2>/dev/null\n");
        s.append("ip link set $CELL_IF txqueuelen ").append(qlenFor(mode)).append(" 2>/dev/null\n");
        s.append("echo \"[ MODE ] Seluler ").append(mode).append(" - IPv6 ").append(ipv6.equals("1") ? "OFF" : "ON").append("\"\n");
        s.append("echo \"[ DONE ] Interface: $CELL_IF\"\n");
        return s.toString();
    }

    public static String statusScript() {
        return HEADER +
                "echo \"==== NETWORK STATUS ====\"\n" +
                "cong=$(cat /proc/sys/net/ipv4/tcp_congestion_control)\n" +
                "ll=$(cat /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null || echo 0)\n" +
                "rmem=$(cat /proc/sys/net/core/rmem_max)\n" +
                "wmem=$(cat /proc/sys/net/core/wmem_max)\n" +
                "if [ \"$cong\" = reno ] && [ \"$ll\" = 1 ]; then ACTIVE=Gaming;\n" +
                "elif [ \"$cong\" = cubic ] && [ \"$rmem\" = 16777216 ] && [ \"$wmem\" = 16777216 ]; then ACTIVE=Download;\n" +
                "elif [ \"$cong\" = cubic ] && [ \"$rmem\" = 8388608 ]; then ACTIVE=Streaming;\n" +
                "elif [ \"$cong\" = cubic ] && [ \"$wmem\" = 16777216 ] && [ \"$rmem\" = 4194304 ]; then ACTIVE=Upload;\n" +
                "else ACTIVE=Default; fi\n" +
                "echo \"Mode aktif  : $ACTIVE\"\n" +
                "echo \"Congestion  : $cong\"\n" +
                "echo \"Low latency : $ll\"\n" +
                "echo \"Slow start  : $(cat /proc/sys/net/ipv4/tcp_slow_start_after_idle)\"\n" +
                "echo \"Fast open   : $(cat /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null)\"\n" +
                "echo \"Timestamps  : $(cat /proc/sys/net/ipv4/tcp_timestamps)\"\n" +
                "echo \"IPv6 disable: $(cat /proc/sys/net/ipv6/conf/all/disable_ipv6)\"\n" +
                "echo \"rmem_max    : $rmem\"\n" +
                "echo \"wmem_max    : $wmem\"\n" +
                "echo \"-- WiFi ($WIFI_IF) --\"\n" +
                "echo \"State : $(ip link show $WIFI_IF 2>/dev/null | grep -oE 'state [A-Z]+' | head -1)\"\n" +
                "echo \"MTU   : $(ip link show $WIFI_IF 2>/dev/null | grep -oE 'mtu [0-9]+' | grep -oE '[0-9]+')\"\n" +
                "echo \"qlen  : $(ip link show $WIFI_IF 2>/dev/null | grep -oE 'qlen [0-9]+' | grep -oE '[0-9]+')\"\n" +
                "echo \"PSave : $(iw dev $WIFI_IF get power_save 2>/dev/null | grep -oE 'on|off' || echo N/A)\"\n" +
                "echo \"-- Seluler ($CELL_IF) --\"\n" +
                "echo \"State : $(ip link show $CELL_IF 2>/dev/null | grep -oE 'state [A-Z]+' | head -1)\"\n" +
                "echo \"MTU   : $(ip link show $CELL_IF 2>/dev/null | grep -oE 'mtu [0-9]+' | grep -oE '[0-9]+')\"\n" +
                "echo \"qlen  : $(ip link show $CELL_IF 2>/dev/null | grep -oE 'qlen [0-9]+' | grep -oE '[0-9]+')\"\n" +
                "echo \"Backup: $([ -f $BACKUP ] && echo tersimpan || echo 'tidak ada')\"\n" +
                "echo \"========================\"\n";
    }

    public static String restoreScript() {
        return HEADER +
                "if [ -f \"$BACKUP\" ]; then\n" +
                "  . \"$BACKUP\"\n" +
                "  echo \"$tcp_congestion\" > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n" +
                "  echo \"$tcp_rmem\"       > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n" +
                "  echo \"$tcp_wmem\"       > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n" +
                "  echo \"$rmem_max\"       > /proc/sys/net/core/rmem_max 2>/dev/null\n" +
                "  echo \"$wmem_max\"       > /proc/sys/net/core/wmem_max 2>/dev/null\n" +
                "  echo \"$tcp_slow_start\" > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n" +
                "  echo \"$tcp_fastopen\"   > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n" +
                "  echo \"$tcp_timestamps\" > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n" +
                "  echo \"$ipv6_disable\"   > /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null\n" +
                "  rm -f \"$BACKUP\"\n" +
                "  echo \"[ RESTORE ] Default dipulihkan dari backup\"\n" +
                "else\n" +
                "  echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null\n" +
                "  echo '1730560 3461120 6922240' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null\n" +
                "  echo '524288 1048576 4525824'  > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null\n" +
                "  echo 8388608 > /proc/sys/net/core/rmem_max 2>/dev/null\n" +
                "  echo 8388608 > /proc/sys/net/core/wmem_max 2>/dev/null\n" +
                "  echo 0 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null\n" +
                "  echo 1 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null\n" +
                "  echo 1 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null\n" +
                "  echo 1 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null\n" +
                "  echo 0 > /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null\n" +
                "  echo \"[ RESTORE ] Tidak ada backup - hard default diterapkan\"\n" +
                "fi\n" +
                "ip link set $WIFI_IF txqueuelen 3000 mtu 1500 2>/dev/null\n" +
                "ip link set $CELL_IF txqueuelen 1000 mtu 1500 2>/dev/null\n" +
                "iw dev $WIFI_IF set power_save on 2>/dev/null\n" +
                "echo \"[ DONE ] Restore selesai\"\n";
    }
}
