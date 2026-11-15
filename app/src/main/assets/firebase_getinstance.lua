function after(hook, param)
    local result = param:getResult()
    if result == nil or
            result.setAnalyticsCollectionEnabled == nil or
            type(result.setAnalyticsCollectionEnabled) ~= 'function' then
        return false
    end

    result:setAnalyticsCollectionEnabled(false)
    return true
end
