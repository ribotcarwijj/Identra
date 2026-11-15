function after(hook, param)
    local result = param:getResult()
    if result == nil or
            result.isDryRunEnabled == nil or type(result.isDryRunEnabled) ~= 'function' or
            result.setDryRun == nil or type(result.setDryRun) ~= 'function' then
        return false
    end

    if result:isDryRunEnabled() then
        return false
    else
        result:setDryRun(true)
        return true
    end
end
