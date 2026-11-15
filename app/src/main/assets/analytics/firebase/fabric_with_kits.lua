function after(hook, param)
    local kits = param:getArgument(1)
    if kits == nil then
        return false
    end

    for index = 1, kits['length'] do
        local kit = kits[index]
        if kit ~= nil and kit.getIdentifier ~= nil and type(kit.getIdentifier) == 'function' then
            local identifier = kit:getIdentifier()
            log(identifier)
            if identifier == 'com.crashlytics.sdk.android:crashlytics' then
                log(kit)
                if kit.core ~= nil and kit.core.disabled ~= nil then
                    kit.core.disabled = true
                    return true
                elseif kit.disabled ~= nil then
                    kit.disabled = true
                    return true
                else
                    log('Crashlytics not disabled')
                end
            end
        end
    end

    return false
end
