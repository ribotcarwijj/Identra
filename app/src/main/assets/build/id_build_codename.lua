function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

    -- profile key android.etc.build.rom.version.codename (ro.build.version.codename), legacy android.build.dev.codename
    local fake = param:getSettingReMap("android.etc.build.rom.version.codename", "android.build.dev.codename", "REL")
    if fake == nil then
        return false
    end

    param:setResult(fake)
    return true, ret, fake
end