package me.diax.diax.listeners

import br.com.brjdevs.java.utils.texts.StringUtils
import me.diax.comportment.jdacommand.CommandHandler
import me.diax.diax.data.ManagedDatabase
import me.diax.diax.data.config.entities.Config
import me.diax.diax.shards.DiaxShard
import me.diax.diax.util.Emote
import me.diax.diax.util.WebHookUtil
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MessageListener(
    private val shard: DiaxShard,
    private val db: ManagedDatabase,
    private val handler: CommandHandler,
    private val config: Config
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.message.isWebhookMessage) return
        if (config.blacklist.contains(event.author.id)) return

        shard.commandPool.submit { onCommand(event) }
    }

    private fun onCommand(event: MessageReceivedEvent) {
        val raw = event.message.rawContent

        for (prefix in config.prefixes) {
            if (raw.startsWith(prefix)) {
                process(event, raw.substring(prefix.length))
                return
            }
        }

        val mentions = listOf("<@${event.author.id}> ", "<@!${event.author.id}> ")
        for (mention in mentions) {
            if (raw.startsWith(mention)) {
                process(event, raw.substring(mention.length))
                return
            }
        }

        val guildPrefix = db[event.guild].settings.prefix
        if (guildPrefix != null && raw.startsWith(guildPrefix)) {
            process(event, raw.substring(guildPrefix.length))
            return
        }
    }

    private fun process(event: MessageReceivedEvent, content: String) {
        val split = StringUtils.efficientSplitArgs(content, 2)
        val cmd = split[0]
        val args = split[1]

        try {
            val command = handler.findCommand(cmd) ?: return
            if (command.hasAttribute("developer") && !config.developers.contains(event.author.id))
                return
            if (command.hasAttribute("fun") || command.hasAttribute("image") || command.hasAttribute("information")) {
            }
            if ((command.hasAttribute("action") || command.hasAttribute("music")) && event.channelType != ChannelType.TEXT) {
                return  // ERROR: ONLY GUILD
            }
            if (command.hasAttribute("patreon") && !(config.donors.contains(event.author.id) || config.developers.contains(event.author.id))) {
                event.channel.sendMessage(Emote.X + " - This is a Patreon-only command.").queue()
                return
            }
            if (event.channelType == ChannelType.PRIVATE && command.hasAttribute("private")) {
                event.channel.sendMessage(Emote.X + " - This command does not work in private messages.").queue()
                return
            }
            handler.execute(command, event.message, args)
        } catch (ignored: PermissionException) {
        } catch (e: Exception) {
            try {
                event.channel.sendMessage(Emote.X + " - Something went wrong that we didn't know about ;-;\nJoin here for help: https://discord.gg/PedN8U").queue()
            } catch (ignored: Exception) {
            }

            e.printStackTrace()
            WebHookUtil.log(event.jda, Emote.X + " An exception occurred.", "An uncaught exception occurred when trying to run: ```" + (handler.findCommand(cmd).description.name + " | " + event.guild + " | " + event.channel).replace("`", "\\`") + "```")
        }
    }
}