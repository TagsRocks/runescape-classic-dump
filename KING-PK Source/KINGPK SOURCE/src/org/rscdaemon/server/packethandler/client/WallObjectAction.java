package org.rscdaemon.server.packethandler.client;

import org.rscdaemon.server.packethandler.PacketHandler;
import org.rscdaemon.server.model.*;
import org.rscdaemon.server.net.Packet;
import org.rscdaemon.server.net.RSCPacket;
import org.rscdaemon.server.entityhandling.defs.DoorDef;
import org.rscdaemon.server.entityhandling.EntityHandler;
import org.rscdaemon.server.event.WalkToPointEvent;
import org.rscdaemon.server.event.ShortEvent;
import org.rscdaemon.server.states.Action;
import org.apache.mina.common.IoSession;

public class WallObjectAction implements PacketHandler {
	/**
	 * World instance
	 */
	public static final World world = World.getWorld();

	public void handlePacket(Packet p, IoSession session) throws Exception {
		Player player = (Player)session.getAttachment();
		int pID = ((RSCPacket)p).getID();
		if(player.isBusy()) {
			player.resetPath();
			return;
		}
		player.resetAll();
		final GameObject object = world.getTile(p.readShort(), p.readShort()).getGameObject();
		final int click = pID == 126 ? 0 : 1;
		if(object == null) {
			player.setSuspiciousPlayer(true);
			return;
		}
		player.setStatus(Action.USING_DOOR);
		world.getDelayedEventHandler().add(new WalkToPointEvent(player, object.getLocation(), 1, false) {
			private void replaceGameObject(int newID, boolean open) {
				world.registerGameObject(new GameObject(object.getLocation(), newID, object.getDirection(), object.getType()));
				owner.getActionSender().sendSound(open ? "opendoor" : "closedoor");
			}
			
			private void doDoor() {
				owner.getActionSender().sendSound("opendoor");
				world.registerGameObject(new GameObject(object.getLocation(), 11, object.getDirection(), object.getType()));
				world.delayedSpawnObject(object.getLoc(), 1000);
			}
			
			public void arrived() {
				owner.resetPath();
				DoorDef def = object.getDoorDef();
				if(owner.isBusy() || owner.isRanging() || !owner.nextTo(object) || def == null || owner.getStatus() != Action.USING_DOOR) {
					return;
				}
				owner.resetAll();
				String command = (click == 0 ? def.getCommand1() : def.getCommand2()).toLowerCase();
				Point telePoint = EntityHandler.getObjectTelePoint(object.getLocation(), command);
				if(telePoint != null) {
					owner.teleport(telePoint.getX(), telePoint.getY(), false);
				}
				else {
					switch(object.getID()) {
						case 1:
							replaceGameObject(2, false);
							break;
						case 2:
							replaceGameObject(1, true);
							break;
						case 9:
							replaceGameObject(8, false);
							break;
						case 8:
							replaceGameObject(9, true);
							break;
						case 23:
							owner.getActionSender().sendMessage("The door is locked");
							break;
						case 112: // Fishing Guild Door
							if(object.getX() != 586 || object.getY() != 524) {
								break;
							}
							if(owner.getY() > 523) {
								if(owner.getCurStat(10) < 68) {
									owner.setBusy(true);
									Npc masterFisher = world.getNpc(368, 582, 588, 524, 527);
									if(masterFisher != null) {
										owner.informOfNpcMessage(new ChatMessage(masterFisher, "Hello only the top fishers are allowed in here", owner));
									}
									world.getDelayedEventHandler().add(new ShortEvent(owner) {
			      						public void action() {
			      							owner.setBusy(false);
			      							owner.getActionSender().sendMessage("You need a fishing level of 68 to enter");
			      						}
			      					});
								}
								else {
									doDoor();
									owner.teleport(586, 523, false);
								}
							}
							else {
								doDoor();
								owner.teleport(586, 524, false);
							}
							break;
						case 55: // Mining Guild Door
							if(object.getX() != 268 || object.getY() != 3381) {
								break;
							}
							if(owner.getY() <= 3380) {
								if(owner.getCurStat(14) < 66) {
									owner.setBusy(true);
									Npc dwarf = world.getNpc(191, 265, 270, 3379, 3380);
									if(dwarf != null) {
										owner.informOfNpcMessage(new ChatMessage(dwarf, "Hello only the top miners are allowed in here", owner));
									}
									world.getDelayedEventHandler().add(new ShortEvent(owner) {
			      						public void action() {
			      							owner.setBusy(false);
			      							owner.getActionSender().sendMessage("You need a mining level of 66 to enter");
			      						}
			      					});
								}
								else {
									doDoor();
									owner.teleport(268, 3381, false);
								}
							}
							else {
								doDoor();
								owner.teleport(268, 3380, false);
							}
							break;
						case 68: // Crafting Guild Door
							if(object.getX() != 347 || object.getY() != 601) {
								return;
							}
							if(owner.getY() <= 600) {
								if(owner.getCurStat(12) < 40) {
									owner.setBusy(true);
									Npc master = world.getNpc(231, 341, 349, 599, 612);
									if(master != null) {
										owner.informOfNpcMessage(new ChatMessage(master, "Hello only the top crafters are allowed in here", owner));
									}
									world.getDelayedEventHandler().add(new ShortEvent(owner) {
				      						public void action() {
				      							owner.setBusy(false);
				      							owner.getActionSender().sendMessage("You need a crafting level of 40 to enter");
				      						}
			      						});
								}
								else if(!owner.getInventory().wielding(191)) {
									Npc master = world.getNpc(231, 341, 349, 599, 612);
									if(master != null) {
										owner.informOfNpcMessage(new ChatMessage(master, "Where is your apron?", owner));
									}
								}
								else {
									doDoor();
									owner.teleport(347, 601, false);
								}
							}
							else {
								doDoor();
								owner.teleport(347, 600, false);
							}
							break;
						case 43: // Cooking Guild Door
							if(object.getX() != 179 || object.getY() != 488) {
								break;
							}
							if(owner.getY() >= 488) {
								if(owner.getCurStat(7) < 32) {
									owner.setBusy(true);
									Npc chef = world.getNpc(133, 176, 181, 480, 487);
									if(chef != null) {
										owner.informOfNpcMessage(new ChatMessage(chef, "Hello only the top cooks are allowed in here", owner));
									}
									world.getDelayedEventHandler().add(new ShortEvent(owner) {
				      						public void action() {
				      							owner.setBusy(false);
				      							owner.getActionSender().sendMessage("You need a cooking level of 32 to enter");
				      						}
			      						});
								}
								else if(!owner.getInventory().wielding(192)) {
									Npc chef = world.getNpc(133, 176, 181, 480, 487);
									if(chef != null) {
										owner.informOfNpcMessage(new ChatMessage(chef, "Where is your chef's hat?", owner));
									}
								}
								else {
									doDoor();
									owner.teleport(179, 487, false);
								}
							}
							else {
								doDoor();
								owner.teleport(179, 488, false);
							}
							break;
						case 146: // Magic Guild Door
							if(object.getX() != 599 || object.getY() != 757) {
								break;
							}
							if(owner.getX() <= 598) {
								if(owner.getCurStat(6) < 66) {
									owner.setBusy(true);
									Npc wizard = world.getNpc(513, 596, 597, 755, 758);
									if(wizard != null) {
										owner.informOfNpcMessage(new ChatMessage(wizard, "Hello only the top wizards are allowed in here", owner));
									}
									world.getDelayedEventHandler().add(new ShortEvent(owner) {
			      						public void action() {
			      							owner.setBusy(false);
			      							owner.getActionSender().sendMessage("You need a magic level of 66 to enter");
			      						}
			      					});
								}
								else {
									doDoor();
									owner.teleport(599, 757, false);
								}
							}
							else {
								doDoor();
								owner.teleport(598, 757, false);
							}
							break;
						case 74: // Heroes guild door
							if(object.getX() != 372 || object.getY() != 441) {
								return;
							}
							doDoor();
							if(owner.getY() >= 441) {
								owner.teleport(372, 440, false);
							}
							else {
								owner.teleport(372, 441, false);
							}
							break;
						case 22: // edge dungeon wall
							if(object.getX() == 219 && object.getY() == 3282) {
								owner.getActionSender().sendSound("secretdoor");
								world.unregisterGameObject(object);
								world.delayedSpawnObject(object.getLoc(), 1000);
								owner.getActionSender().sendMessage("You just went through a secret door");
								if(owner.getX() <= 218) {
									owner.teleport(219, 3282, false);
								}
								else {
									owner.teleport(218, 3282, false);
								}
							}
							else {
								owner.getActionSender().sendMessage("Nothing interesting happens");
							}
							break;
						case 58: // Karamja -> cranador wall
							if(object.getX() != 406 || object.getY() != 3518) {
								return;
							}
							doDoor();
							if(owner.getY() <= 3517) {
								owner.teleport(406, 3518, false);
							}
							else {
								owner.teleport(406, 3517, false);
							}
							break;
						case 101: // Woodcutting guild secret exit
							if(object.getX() != 540 || object.getY() != 445) {
								return;
							}
							if(owner.getX() >= 540) {
								owner.getActionSender().sendMessage("You push your way through");
								owner.teleport(539, 445, false);
							}
							else {
								owner.getActionSender().sendMessage("You can't seem to get through");
							}
							break;
						case 38: // Black Knight Guard Door
							if(object.getX() != 271 || object.getY() != 441) {
								return;
							}
							if(owner.getX() <= 270) {
								if(!owner.getInventory().wielding(7) || !owner.getInventory().wielding(104)) {
									owner.getActionSender().sendMessage("Only guards are allowed in there!");
									return;
								}
								doDoor();
								owner.teleport(271, 441, false);
							}
							else {
								doDoor();
								owner.teleport(270, 441, false);
							}
							break;
						case 36: // Draynor mansion front door
							if(object.getX() != 210 || object.getY() != 553) {
								return;
							}
							if(owner.getY() >= 553) {
								doDoor();
								owner.teleport(210, 552, false);
							}
							else {
								owner.getActionSender().sendMessage("The door is locked shut");
							}
							break;
						case 37: // Draynor mansion back door
							if(object.getX() != 199 || object.getY() != 551) {
								return;
							}
							if(owner.getY() >= 551) {
								doDoor();
								owner.teleport(199, 550, false);
							}
							else {
								owner.getActionSender().sendMessage("The door is locked shut");
							}
							break;
						case 60: // Melzars made (coming out only)
							if(owner.getX() > 337) {
								doDoor();
								owner.teleport(337, owner.getY(), false);
							}
							else {
								owner.getActionSender().sendMessage("The door is locked shut");
							}
							break;
						case 30: // Locked Doors
							owner.getActionSender().sendMessage("The door is locked shut");
							break;
						default:
							owner.getActionSender().sendMessage("Nothing interesting happens.");
							break;
					}
				}
			}
		});
	}
	
}