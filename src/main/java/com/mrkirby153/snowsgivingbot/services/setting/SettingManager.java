package com.mrkirby153.snowsgivingbot.services.setting;

import com.mrkirby153.snowsgivingbot.entity.SettingEntity;
import com.mrkirby153.snowsgivingbot.entity.repo.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingManager implements SettingService {

    private final SettingsRepository settingsRepository;

    @Override
    @CacheEvict(cacheNames = "settings", key = "#p0.getKey()+'-'+#p1.getId()")
    public <T> void set(GuildSetting<T> setting, Guild guild, T value) {
        set(setting, guild.getId(), value);
    }

    @Override
    @CacheEvict(cacheNames = "settings", key = "#p0.getKey()+'-'+#p1")
    public <T> void set(GuildSetting<T> setting, String guildId, T value) {
        final String key = setting.getKey();
        log.debug("Setting {} = {} on {}", key, value, guildId);
        Optional<SettingEntity> existing = settingsRepository
            .getByGuildAndKey(guildId, setting.getKey());
        existing.ifPresent(e -> {
            if ((setting.getDefaultSetting() != null && setting.getDefaultSetting().equals(value)) || (setting.getDefaultSetting() == null && value == null)) {
                log.debug("Deleting {} on {} because it's being reset to default", key,
                    guildId);
                settingsRepository.delete(e);
            } else {
                e.setValue(setting.serialize(value));
                settingsRepository.save(e);
            }
        });
        if (!existing.isPresent()) {
            log.debug("Creating {} on {}", key, guildId);
            SettingEntity entity = new SettingEntity(guildId, key,
                setting.serialize(value));
            settingsRepository.save(entity);
        }
    }

    @Override
    @Cacheable(cacheNames = "settings", key = "#p0.getKey()+'-'+#p1.getId()")
    public <T> T get(GuildSetting<T> setting, Guild guild) {
        return get(setting, guild.getId());
    }

    @Override
    @Cacheable(cacheNames = "settings", key = "#p0.getKey()+'-'+#p1")
    public <T> T get(GuildSetting<T> setting, String guildId) {
        log.debug("Retrieving {} on {} from the db", setting.getKey(), guildId);
        Optional<T> existing = settingsRepository.getByGuildAndKey(guildId, setting.getKey())
            .map(e -> setting.deserialize(e.getValue()));
        return existing.orElse(setting.getDefaultSetting());
    }
}