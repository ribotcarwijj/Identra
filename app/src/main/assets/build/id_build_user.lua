function after(hook, param)
	local ret = param:getResult()
	if ret == nil then
		return false
	end

    -- 0.7.3: same setting as Build.USER/Rule (ro.build.user); was android.etc.build.rom.base.os by mistake
    local fake = param:getSetting("android.etc.build.rom.user", nil)
    if fake == nil or fake == "" then fake = param:getSetting("android.build.user", nil) end
    if fake == nil or fake == "" then
        return false
    end
    param:setResult(fake)
    return true, ret, fake
end