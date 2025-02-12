package org.rscdaemon.ls;

import org.rscdaemon.ls.model.World;
import org.rscdaemon.ls.net.FPacket;
import org.rscdaemon.ls.net.LSPacket;
import org.rscdaemon.ls.net.PacketQueue;
import org.rscdaemon.ls.packethandler.PacketHandler;
import org.rscdaemon.ls.packethandler.PacketHandlerDef;
import org.rscdaemon.ls.util.PersistenceManager;

import java.util.List;
import java.util.TreeMap;

public class LoginEngine extends Thread {
	/**
	 * The main server
	 */
	private Server server;
	/**
	 * The packet queue to be processed
	 */
	private PacketQueue<LSPacket> LSPacketQueue;
	/**
	 * The packet queue to be processed
	 */
	private PacketQueue<FPacket> FPacketQueue;
	/**
	 * Should we be running?
	 */
	private boolean running = true;
	/**
	 * The mapping of packet IDs to their handler
	 */
	private TreeMap<Integer, PacketHandler> LSPacketHandlers = new TreeMap<Integer, PacketHandler>();
	/**
	 * The mapping of packet IDs to their handler
	 */
	private TreeMap<Integer, PacketHandler> FPacketHandlers = new TreeMap<Integer, PacketHandler>();
	/**
	 * The mapping of packet UIDs to their handler
	 */
	private TreeMap<Long, PacketHandler> uniqueHandlers = new TreeMap<Long, PacketHandler>();
	
	public LoginEngine(Server server) {
		this.server = server;
		LSPacketQueue = new PacketQueue<LSPacket>();
		FPacketQueue = new PacketQueue<FPacket>();
		loadPacketHandlers();
	}
	
	public void run() {
		System.out.println("LoginEngine now running");
		while (running) {
			try { Thread.sleep(50); } catch(InterruptedException ie) {}
			processIncomingPackets();
			processOutgoingPackets();
		}
	}
	
	public void setHandler(long uID, PacketHandler handler) {
		uniqueHandlers.put(uID, handler);
	}
	
	public PacketQueue<LSPacket> getLSPacketQueue() {
		return LSPacketQueue;
	}
	
	public PacketQueue<FPacket> getFPacketQueue() {
		return FPacketQueue;
	}
	
	public void processOutgoingPackets() {
		for(World w : server.getWorlds()) {
			List<LSPacket> packets = w.getActionSender().getPackets();
			for(LSPacket packet : packets) {
				w.getSession().write(packet);
			}
			w.getActionSender().clearPackets();
		}
	}
	
	/**
	 * Loads the packet handling classes from the persistence
	 * manager.
	 */
	protected void loadPacketHandlers() {
		PacketHandlerDef[] handlerDefs = (PacketHandlerDef[])PersistenceManager.load("LSPacketHandlers.xml");
		for(PacketHandlerDef handlerDef : handlerDefs) {
			try {
				String className = handlerDef.getClassName();
				Class c = Class.forName(className);
				if (c != null) {
					PacketHandler handler = (PacketHandler)c.newInstance();
					for(int packetID : handlerDef.getAssociatedPackets()) {
						LSPacketHandlers.put(packetID, handler);
					}
				}
			}
			catch (Exception e) {
				Server.error(e);
			}
		}
		handlerDefs = (PacketHandlerDef[])PersistenceManager.load("FPacketHandlers.xml");
		for(PacketHandlerDef handlerDef : handlerDefs) {
			try {
				String className = handlerDef.getClassName();
				Class c = Class.forName(className);
				if (c != null) {
					PacketHandler handler = (PacketHandler)c.newInstance();
					for(int packetID : handlerDef.getAssociatedPackets()) {
						FPacketHandlers.put(packetID, handler);
					}
				}
			}
			catch (Exception e) {
				Server.error(e);
			}
		}
	}
	
	/**
	 * Processes incoming packets.
	 */
	private void processIncomingPackets() {
		for(LSPacket p : LSPacketQueue.getPackets()) {
			PacketHandler handler;
			if(((handler = uniqueHandlers.get(p.getUID())) != null) || ((handler = LSPacketHandlers.get(p.getID())) != null)) {
				try {
					handler.handlePacket(p, p.getSession());
					uniqueHandlers.remove(p.getUID());
				}
				catch(Exception e) {
					Server.error("Exception with p[" + p.getID() + "]: " + e);
				}
			}
			else {
				Server.error("Unhandled packet from server: " + p.getID());
			}
		}
		for(FPacket p : FPacketQueue.getPackets()) {
			PacketHandler handler = FPacketHandlers.get(p.getID());
			if(handler != null) {
				try {
					handler.handlePacket(p, p.getSession());
				}
				catch(Exception e) {
					Server.error("Exception with p[" + p.getID() + "]: " + e);
				}
			}
			else {
				Server.error("Unhandled packet from frontend: " + p.getID());
			}
		}
	}
}