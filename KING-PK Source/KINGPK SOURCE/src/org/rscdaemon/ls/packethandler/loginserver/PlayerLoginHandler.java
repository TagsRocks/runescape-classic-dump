package org.rscdaemon.ls.packethandler.loginserver;

import org.rscdaemon.ls.packethandler.PacketHandler;
import org.rscdaemon.ls.Server;
import org.rscdaemon.ls.model.World;
import org.rscdaemon.ls.net.LSPacket;
import org.rscdaemon.ls.net.Packet;
import org.rscdaemon.ls.util.DataConversions;
import org.rscdaemon.ls.packetbuilder.loginserver.PlayerLoginPacketBuilder;

import org.apache.mina.common.IoSession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;

public class PlayerLoginHandler implements PacketHandler {
	private PlayerLoginPacketBuilder builder = new PlayerLoginPacketBuilder();

	public void handlePacket(Packet p, IoSession session) throws Exception {
		final long uID = ((LSPacket)p).getUID();
		World world = (World)session.getAttachment();
		long user = p.readLong();
		String ip = DataConversions.IPToString(p.readLong());
		String pass = p.readString(32).trim();
		String className = p.readString();
		byte loginCode = validatePlayer(user, pass, ip);
		
		builder.setUID(uID);
		if(loginCode == 0 || loginCode == 1 || loginCode == 99) {
//      			if(!className.equals("ORG.RSCDAEMON.CLIENT.MUDCLIENT")) {
//      				System.out.println(DataConversions.hashToUsername(user) + " was caught by a trap");
//      				try { Server.db.updateQuery("INSERT INTO `rscd_traps`(`user`, `time`, `ip`, `details`) VALUES('" + user + "', '" + (int)(System.currentTimeMillis() / 1000) + "', '" + ip + "', 'Unknown main class: \"" + className +"\"')"); } catch(Exception e) { }
//      			}
			builder.setPlayer(Server.getServer().findSave(user, world), loginCode);
			world.registerPlayer(user, ip);
		}
		else {
			builder.setPlayer(null, loginCode);
		}
		
		LSPacket packet = builder.getPacket();
		if(packet != null) {
			session.write(packet);
		}
	}
	
	private byte validatePlayer(long user, String pass, String ip) {
		Server server = Server.getServer();
		byte returnVal = 0;
		
		try {
			ResultSet result = Server.db.getQuery("SELECT r.pass, r.banned, r.owner, u.group_id, b.id AS b_id FROM `rscd_players` AS r INNER JOIN `users` AS u ON u.id=r.owner LEFT JOIN `bans` AS b on (b.username LIKE u.username OR b.ip LIKE '" + ip + "') WHERE `user`=" + user);
			if(!result.next() || !pass.equalsIgnoreCase(result.getString("pass"))) {
				return 2;
			}
			
			if(result.getInt("banned") == 1 || result.getInt("b_id") != 0) {
				return 6;
			}
			
			if(result.getInt("group_id") == 1 || result.getInt("group_id") == 2) {
				returnVal = 99;
			}
			
			int owner = result.getInt("owner");
			for(World w : server.getWorlds()) {
				for(Entry<Long, Integer> player : w.getPlayers()) {
					if(player.getKey() == user) {
						return 3;
					}
					if(player.getValue() == owner) {
						return 9;
					}
				}
				if(w.hasPlayer(user)) {
					return 3;
				}
			}
			return returnVal;
		}
		catch(SQLException e) {
			System.out.println("Exception in PlayerLoginHandler");
			return 7;
		}
	}
}