package com.github.andreyasadchy.xtra.di

import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragment
import com.github.andreyasadchy.xtra.ui.chat.ChatFragment
import com.github.andreyasadchy.xtra.ui.clips.common.ClipsFragment
import com.github.andreyasadchy.xtra.ui.download.ClipDownloadDialog
import com.github.andreyasadchy.xtra.ui.download.VideoDownloadDialog
import com.github.andreyasadchy.xtra.ui.follow.FollowPagerFragment
import com.github.andreyasadchy.xtra.ui.follow.channels.FollowedChannelsFragment
import com.github.andreyasadchy.xtra.ui.follow.games.FollowedGamesFragment
import com.github.andreyasadchy.xtra.ui.games.GamesFragment
import com.github.andreyasadchy.xtra.ui.player.clip.ClipPlayerFragment
import com.github.andreyasadchy.xtra.ui.player.games.PlayerGamesFragment
import com.github.andreyasadchy.xtra.ui.player.offline.OfflinePlayerFragment
import com.github.andreyasadchy.xtra.ui.player.stream.StreamPlayerFragment
import com.github.andreyasadchy.xtra.ui.player.video.VideoPlayerFragment
import com.github.andreyasadchy.xtra.ui.saved.SavedPagerFragment
import com.github.andreyasadchy.xtra.ui.saved.bookmarks.BookmarksFragment
import com.github.andreyasadchy.xtra.ui.saved.downloads.DownloadsFragment
import com.github.andreyasadchy.xtra.ui.search.SearchFragment
import com.github.andreyasadchy.xtra.ui.search.channels.ChannelSearchFragment
import com.github.andreyasadchy.xtra.ui.search.games.GameSearchFragment
import com.github.andreyasadchy.xtra.ui.search.streams.StreamSearchFragment
import com.github.andreyasadchy.xtra.ui.search.tags.BaseTagSearchFragment
import com.github.andreyasadchy.xtra.ui.search.tags.TagSearchFragment
import com.github.andreyasadchy.xtra.ui.search.videos.VideoSearchFragment
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.ui.streams.common.StreamsFragment
import com.github.andreyasadchy.xtra.ui.streams.followed.FollowedStreamsFragment
import com.github.andreyasadchy.xtra.ui.videos.channel.ChannelVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.followed.FollowedVideosFragment
import com.github.andreyasadchy.xtra.ui.videos.game.GameVideosFragment
import com.github.andreyasadchy.xtra.ui.view.chat.MessageClickedDialog
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBuilderModule {

    @ContributesAndroidInjector
    abstract fun contributeSettingsFragment(): SettingsActivity.SettingsFragment

    @ContributesAndroidInjector
    abstract fun contributeGamesFragment(): GamesFragment

    @ContributesAndroidInjector
    abstract fun contributeFollowedStreamsFragment(): FollowedStreamsFragment

    @ContributesAndroidInjector
    abstract fun contributeStreamsFragment(): StreamsFragment

    @ContributesAndroidInjector
    abstract fun contributeStreamPlayerFragment(): StreamPlayerFragment

    @ContributesAndroidInjector
    abstract fun contributeVideoPlayerFragment(): VideoPlayerFragment

    @ContributesAndroidInjector
    abstract fun contributeClipPlayerFragment(): ClipPlayerFragment

    @ContributesAndroidInjector
    abstract fun contributeOfflinePlayerFragment(): OfflinePlayerFragment

    @ContributesAndroidInjector
    abstract fun contributeClipsFragment(): ClipsFragment

    @ContributesAndroidInjector
    abstract fun contributeChannelVideosFragment(): ChannelVideosFragment

    @ContributesAndroidInjector
    abstract fun contributeVideosFragment(): GameVideosFragment

    @ContributesAndroidInjector
    abstract fun contributeFollowedVideosFragment(): FollowedVideosFragment

    @ContributesAndroidInjector
    abstract fun contributeDownloadsFragment(): DownloadsFragment

    @ContributesAndroidInjector
    abstract fun contributeBookmarksFragment(): BookmarksFragment

    @ContributesAndroidInjector
    abstract fun contributeVideoDownloadDialog(): VideoDownloadDialog

    @ContributesAndroidInjector
    abstract fun contributeClipDownloadDialog(): ClipDownloadDialog

    @ContributesAndroidInjector
    abstract fun contributeSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    abstract fun contributeChannelSearchFragment(): ChannelSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeGameSearchFragment(): GameSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeVideoSearchFragment(): VideoSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeStreamSearchFragment(): StreamSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeBaseTagSearchFragment(): BaseTagSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeTagSearchFragment(): TagSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeChannelPagerFragment(): ChannelPagerFragment

    @ContributesAndroidInjector
    abstract fun contributeMessageClickedDialog(): MessageClickedDialog

    @ContributesAndroidInjector
    abstract fun contributeChatFragment(): ChatFragment

    @ContributesAndroidInjector
    abstract fun contributeFollowedChannelsFragment(): FollowedChannelsFragment

    @ContributesAndroidInjector
    abstract fun contributeFollowedGamesFragment(): FollowedGamesFragment

    @ContributesAndroidInjector
    abstract fun contributePlayerGamesFragment(): PlayerGamesFragment

    @ContributesAndroidInjector
    abstract fun contributeFollowPagerFragment(): FollowPagerFragment

    @ContributesAndroidInjector
    abstract fun contributeSavedPagerFragment(): SavedPagerFragment
}
