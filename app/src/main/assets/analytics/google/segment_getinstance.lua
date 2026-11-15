function before(hook, param)
    local analytics = param:getArgument(0)
    if analytics == nil or analytics.optOut == nil then
        return false
    end

    if type(analytics.optOut) == 'function' then
        analytics:optOut(true)
        return true
    elseif type(analytics.optOut) == 'userdata' then
        analytics.optOut:set(false)
        return true
    else
        return false
    end
end
