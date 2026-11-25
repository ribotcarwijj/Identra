function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

    -- profile key android.etc.build.rom.variant (ro.build.type), legacy android.build.type
    local fake = param:getSettingReMap("android.etc.build.rom.variant", "android.build.type", "user")
    if fake == nil then
        return false
    end
    param:setResult(fake)
    return true, ret, fake
end