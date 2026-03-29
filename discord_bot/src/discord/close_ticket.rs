use std::fmt::{self, Display, Formatter};

use indoc::formatdoc;
use log::{debug, warn};
use poise::{
    serenity_prelude::{
        ButtonStyle, CacheHttp, ChannelId, CreateButton, CreateEmbed, CreateEmbedFooter,
        CreateMessage, EditThread, ForumTag, Http, Mentionable, ReactionType, Timestamp,
    },
    CreateReply, ReplyHandle,
};

use crate::{check_is_support, webhooks::GetTagId, Context, WinstonError, SUPPORT_ROLE_ID};

#[derive(Debug, poise::ChoiceParameter)]
pub enum CloseReason {
    #[name = "✅ Resolved"]
    Resolved,
    #[name = "⛔ Declined"]
    Declined,
    #[name = "❌ Unreproducible"]
    UnReproducible,
    #[name = "👥 Duplicate"]
    Duplicate,
}

impl CloseReason {
    fn get_tag_name(&self) -> &str {
        match self {
            CloseReason::Resolved => "Resolved",
            CloseReason::Declined => "Declined",
            CloseReason::UnReproducible => "Unreproducible",
            CloseReason::Duplicate => "Duplicate",
        }
    }

    pub fn get_color(&self) -> u32 {
        match self {
            CloseReason::Resolved => 0x53ff52,
            CloseReason::Declined => 0xff5252,
            CloseReason::UnReproducible => 0xFFA500,
            CloseReason::Duplicate => 0x1F85DE,
        }
    }

    pub fn is_close_tag(tag: &ForumTag) -> bool {
        match tag.name.to_lowercase().as_str() {
            "resolved" | "declined" | "unreproducible" | "duplicate" => true,
            _ => false,
        }
    }
}

impl Display for CloseReason {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        match self {
            CloseReason::Resolved => write!(f, "✅ Resolved"),
            CloseReason::Declined => write!(f, "⛔ Declined"),
            CloseReason::UnReproducible => write!(f, "❌ Unreproducible"),
            CloseReason::Duplicate => write!(f, "👥 Duplicate"),
        }
    }
}

#[poise::command(slash_command, ephemeral, check = "check_is_support")]
pub async fn close_ticket(
    ctx: Context<'_>,
    #[description = "The reason for closing the ticket"] reason: CloseReason,
    #[description = "Is the user allowed to reopen the ticket?"] allow_reopen: bool,
) -> Result<(), WinstonError> {
    let handle = ctx
        .send(CreateReply::default().content("Closing ticket..."))
        .await?;

    let mut channel = ctx
        .channel_id()
        .to_channel(ctx)
        .await?
        .guild()
        .ok_or(WinstonError::NotAGuildChannel)?;

    let parent_channel = channel
        .parent_id
        .ok_or(WinstonError::NotAGuildChannel)?
        .to_channel(&ctx)
        .await?
        .guild()
        .ok_or(WinstonError::NotAGuildChannel)?;

    let owner_id = channel.owner_id.ok_or(WinstonError::NotAThreadChannel)?;

    let available_tags = parent_channel.available_tags;
    let Some(tag) = available_tags.get_tag_id(reason.get_tag_name()) else {
        update_responds(
            ctx,
            &handle,
            "The tag for the reason could not be found. Please contact an admin.",
        )
        .await?;
        return Ok(());
    };

    handle.delete(ctx).await?;

    let embed = CreateEmbed::default()
        .title("Ticket Closed")
        .color(reason.get_color())
        .field("Reason", reason.to_string(), false)
        .footer(
            CreateEmbedFooter::new(format!("Closed by {}", ctx.author().name))
                .icon_url(ctx.author().avatar_url().unwrap_or_default()),
        )
        .timestamp(Timestamp::now());

    let embed = if allow_reopen {
        embed.description(formatdoc! {"
            This ticket has been closed.

            *If you feel like this was a mistake, click the button below to reopen the ticket.*
        "})
    } else {
        embed.description("This ticket has been closed.")
    };

    let message = CreateMessage::default()
        .content(format!("{}", owner_id.mention()))
        .embed(embed);

    let message = if allow_reopen {
        message.button(
            CreateButton::new("reopen_ticket")
                .label("Reopen Ticket")
                .style(ButtonStyle::Secondary)
                .emoji(ReactionType::Unicode("🔓".to_string()))
                .disabled(false),
        )
    } else {
        message
    };

    channel.send_message(&ctx, message).await?;

    if let Err(e) = remove_support_members_from_thread(&ctx, channel.id).await {
        warn!("Could not remove members from thread: {e}");
    }

    channel
        .edit_thread(
            ctx,
            EditThread::default()
                .applied_tags(vec![tag])
                .locked(!allow_reopen),
        )
        .await?;

    Ok(())
}

async fn update_responds(
    ctx: Context<'_>,
    handle: &ReplyHandle<'_>,
    message: &str,
) -> Result<(), WinstonError> {
    handle
        .edit(ctx, CreateReply::default().content(message))
        .await?;

    Ok(())
}

pub async fn remove_support_members_from_thread<C>(
    ctx: &C,
    channel_id: ChannelId,
) -> Result<(), WinstonError>
where
    C: CacheHttp + AsRef<Http>,
{
    let channel = channel_id.to_channel(&ctx).await?;
    let guild_channel = channel.guild().ok_or(WinstonError::NotAGuildChannel)?;
    let guild_id = guild_channel.guild_id;

    for thread_member in channel_id.get_thread_members(&ctx).await? {
        let member = guild_id.member(&ctx, thread_member.user_id).await?;
        if !member
            .roles
            .iter()
            .any(|role_id| *role_id == SUPPORT_ROLE_ID)
        {
            continue;
        }

        debug!("Removing member from thread: {}", member);

        if let Err(e) = channel_id
            .remove_thread_member(&ctx, thread_member.user_id)
            .await
        {
            warn!("Could not remove member from thread: {e}");
        }
    }

    Ok(())
}
