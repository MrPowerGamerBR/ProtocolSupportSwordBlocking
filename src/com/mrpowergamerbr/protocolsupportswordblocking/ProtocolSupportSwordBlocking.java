package com.mrpowergamerbr.protocolsupportswordblocking;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mrpowergamerbr.protocolsupportswordblocking.packetwrapper.WrapperPlayClientBlockPlace;
import com.mrpowergamerbr.protocolsupportswordblocking.packetwrapper.WrapperPlayServerEntityMetadata;

import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolVersion;

public class ProtocolSupportSwordBlocking extends JavaPlugin implements Listener {
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

		// Block Place (sword blocking)
		// We don't need to process block dig because it seems that's unaffected (yay!)
		protocolManager.addPacketListener(new PacketAdapter(this,
				ListenerPriority.HIGHEST, 
				new PacketType[] { PacketType.Play.Client.BLOCK_PLACE, PacketType.Play.Server.ENTITY_METADATA }) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (ProtocolSupportAPI.getProtocolVersion(event.getPlayer()).isBefore(ProtocolVersion.MINECRAFT_1_9)) { // If it is a client before MC 1.9...
					if (event.getPacketType() == PacketType.Play.Client.BLOCK_PLACE) { // and it is a block place packet
						event.setCancelled(true); // We are going to cancel this packet...
						WrapperPlayClientBlockPlace wrappedPacket = new WrapperPlayClientBlockPlace(event.getPacket().shallowClone()); // Clone the packet!
						wrappedPacket.setHand(Hand.OFF_HAND); // Change the held item to off hand
						try {
							ProtocolLibrary.getProtocolManager().recieveClientPacket(event.getPlayer(), event.getPacket(), false); // Now we are going to resend the original packet...
							ProtocolLibrary.getProtocolManager().recieveClientPacket(event.getPlayer(), wrappedPacket.getHandle(), false); // ...and simulate a receive client packet
						} catch (InvocationTargetException | IllegalAccessException e) {
							e.printStackTrace();
						} 
					}
				}
			}
			
			@Override
			public void onPacketSending(PacketEvent event) {
				if (ProtocolSupportAPI.getProtocolVersion(event.getPlayer()).isBefore(ProtocolVersion.MINECRAFT_1_9)) { // If it is a client before MC 1.9...
					if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) { // And it is a entity metadata packet
						WrapperPlayServerEntityMetadata wrappedPacket = new WrapperPlayServerEntityMetadata(event.getPacket().deepClone()); // Clone the packet!
						for (WrappedWatchableObject wwo : wrappedPacket.getMetadata()) { // Now let's loop our watchable objects...
							if (wwo.getIndex() == 6) { // Index 6 = Shield
								// Value 3 = Off Hand, Value 1 = Main Hand
								// We are going to set it to 1 (main hand) if the value is 3 (off hand) so we can fake a sword blocking animation for older MC clients
								if (wwo.getHandle() instanceof Byte && (byte) wwo.getValue() == 3) {
									wwo.setValue((byte) 1);
								}
							}
						}
						event.setPacket(wrappedPacket.getHandle());
					}
				}
			}
		});
	}
}
