function before(hook, param)
    local restricted = false;
    local events = param:getArgument(1)
    if bit32.band(events, 16) ~= 0 then -- cell location
        restricted = true
        events = bit32.bxor(events, 16)
    end
    if bit32.band(events, 1024) ~= 0 then -- cell info
        restricted = true
        events = bit32.bxor(events, 1024)
    end
    if restricted then
        param:setArgument(1, events)
    end
    return restricted
end
